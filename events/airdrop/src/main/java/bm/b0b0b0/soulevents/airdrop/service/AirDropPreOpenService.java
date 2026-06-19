package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.PreOpenBeaconSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.PreOpenMobsSettings;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class AirDropPreOpenService {

    private final Plugin plugin;
    private final SoulEventsApi api;
    private final Map<UUID, ActivePreOpen> active = new ConcurrentHashMap<>();

    public AirDropPreOpenService(Plugin plugin, SoulEventsApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    public void start(
            UUID sessionId,
            AirDropTypeDefinition definition,
            Location anchor,
            Instant lootableAt
    ) {
        AirDropTypeSettings type = definition.settings();
        if (!type.preOpenBeacon.enabled && !type.preOpenMobs.enabled) {
            return;
        }
        stop(sessionId);
        ActivePreOpen state = new ActivePreOpen(sessionId, definition, blockAnchor(anchor), lootableAt);
        active.put(sessionId, state);
        if (type.preOpenBeacon.enabled) {
            startBeacon(state);
        }
        if (type.preOpenMobs.enabled) {
            spawnMobWave(state);
            startMobWaves(state);
        }
    }

    public void stop(UUID sessionId) {
        ActivePreOpen state = active.remove(sessionId);
        if (state == null) {
            return;
        }
        if (state.beaconTask != null) {
            state.beaconTask.cancel();
        }
        if (state.mobTask != null) {
            state.mobTask.cancel();
        }
        for (UUID entityId : state.spawnedMobs) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && !entity.isDead()) {
                entity.remove();
            }
        }
        state.spawnedMobs.clear();
    }

    public void shutdown() {
        for (UUID sessionId : active.keySet().stream().toList()) {
            stop(sessionId);
        }
    }

    private void startBeacon(ActivePreOpen state) {
        PreOpenBeaconSettings beacon = state.definition.settings().preOpenBeacon;
        int profileRadius = api.protection().effects().profileRadius(beacon.effectProfileId);
        int radius = beacon.radius > 0 ? beacon.radius : profileRadius;
        long interval = Math.max(10L, api.protection().effects().profileTickInterval(beacon.effectProfileId));
        state.beaconTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Instant.now().isAfter(state.lootableAt)) {
                stop(state.sessionId);
                return;
            }
            playBeaconFx(state.anchor, radius);
            applyBeaconEffects(state.sessionId, state.anchor, radius, beacon.effectProfileId);
        }, 0L, interval);
    }

    private void startMobWaves(ActivePreOpen state) {
        PreOpenMobsSettings mobs = state.definition.settings().preOpenMobs;
        long intervalTicks = Math.max(20L, mobs.waveIntervalSeconds * 20L);
        state.mobTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (Instant.now().isAfter(state.lootableAt)) {
                stop(state.sessionId);
                return;
            }
            cleanupDeadMobs(state);
            if (state.spawnedMobs.size() >= mobs.maxAlive) {
                return;
            }
            spawnMobWave(state);
        }, intervalTicks, intervalTicks);
    }

    private void spawnMobWave(ActivePreOpen state) {
        PreOpenMobsSettings settings = state.definition.settings().preOpenMobs;
        World world = state.anchor.getWorld();
        if (world == null || settings.mobTypes.isEmpty()) {
            return;
        }
        int toSpawn = Math.min(settings.mobCount, Math.max(0, settings.maxAlive - state.spawnedMobs.size()));
        for (int index = 0; index < toSpawn; index++) {
            EntityType entityType = pickMobType(settings.mobTypes);
            if (entityType == null) {
                continue;
            }
            Location spawn = randomSpawnLocation(state.anchor, settings.spawnRadius);
            if (spawn == null) {
                continue;
            }
            Entity entity = world.spawnEntity(spawn, entityType);
            if (entity instanceof Mob mob) {
                mob.setRemoveWhenFarAway(false);
                mob.setPersistent(true);
            }
            if (entity instanceof LivingEntity living) {
                living.setCustomNameVisible(false);
            }
            state.spawnedMobs.add(entity.getUniqueId());
        }
    }

    private void applyBeaconEffects(UUID sessionId, Location anchor, int radius, String profileId) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        List<PotionEffect> baseEffects = api.protection().effects().profileEffects(profileId);
        if (baseEffects.isEmpty()) {
            return;
        }
        double radiusSquared = (double) radius * radius;
        for (Player player : world.getPlayers()) {
            if (!player.getWorld().equals(world)) {
                continue;
            }
            if (player.getLocation().distanceSquared(anchor) > radiusSquared) {
                continue;
            }
            List<PotionEffect> resolved = api.protection().effects().resolve(sessionId, player, baseEffects);
            for (PotionEffect effect : resolved) {
                player.addPotionEffect(effect, true);
            }
        }
    }

    private void playBeaconFx(Location anchor, int radius) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        Location center = anchor.clone().add(0.5, 1.0, 0.5);
        world.spawnParticle(Particle.END_ROD, center, 12, 0.25, 0.8, 0.25, 0.01);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, center, 8, 0.35, 0.15, 0.35, 0.01);
        world.spawnParticle(Particle.DUST, center, 16, 0.4, 0.2, 0.4, 0.0, new Particle.DustOptions(
                org.bukkit.Color.fromRGB(180, 0, 0),
                1.2f
        ));
        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            world.playSound(center, Sound.BLOCK_BEACON_AMBIENT, 0.35f, 0.7f);
        }
    }

    private static void cleanupDeadMobs(ActivePreOpen state) {
        state.spawnedMobs.removeIf(entityId -> {
            Entity entity = Bukkit.getEntity(entityId);
            return entity == null || entity.isDead();
        });
    }

    private static EntityType pickMobType(List<String> mobTypes) {
        List<EntityType> resolved = new ArrayList<>();
        for (String raw : mobTypes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            EntityType type = resolveEntityType(raw);
            if (type != null && type.isAlive()) {
                resolved.add(type);
            }
        }
        if (resolved.isEmpty()) {
            return null;
        }
        return resolved.get(ThreadLocalRandom.current().nextInt(resolved.size()));
    }

    private static EntityType resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(normalized.toLowerCase(Locale.ROOT)));
        }
    }

    private static Location randomSpawnLocation(Location anchor, int radius) {
        World world = anchor.getWorld();
        if (world == null || radius <= 0) {
            return null;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble(Math.PI * 2.0);
            double distance = 4.0 + random.nextDouble(Math.max(4.0, radius));
            int x = anchor.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = anchor.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z, org.bukkit.HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location spawn = new Location(world, x + 0.5, y + 1.0, z + 0.5);
            Material below = spawn.clone().subtract(0.0, 1.0, 0.0).getBlock().getType();
            if (below.isSolid() && spawn.getBlock().getType().isAir()) {
                return spawn;
            }
        }
        return null;
    }

    private static Location blockAnchor(Location anchor) {
        return new Location(
                anchor.getWorld(),
                anchor.getBlockX(),
                anchor.getBlockY(),
                anchor.getBlockZ()
        );
    }

    private static final class ActivePreOpen {
        private final UUID sessionId;
        private final AirDropTypeDefinition definition;
        private final Location anchor;
        private final Instant lootableAt;
        private final Set<UUID> spawnedMobs = new HashSet<>();
        private BukkitTask beaconTask;
        private BukkitTask mobTask;

        private ActivePreOpen(UUID sessionId, AirDropTypeDefinition definition, Location anchor, Instant lootableAt) {
            this.sessionId = sessionId;
            this.definition = definition;
            this.anchor = anchor;
            this.lootableAt = lootableAt;
        }
    }
}
