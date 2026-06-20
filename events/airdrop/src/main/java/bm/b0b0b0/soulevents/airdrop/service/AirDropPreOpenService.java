package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.PreOpenBeaconSettings;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        if (!type.preOpenBeacon.enabled) {
            return;
        }
        stop(sessionId);
        ActivePreOpen state = new ActivePreOpen(sessionId, definition, blockAnchor(anchor), lootableAt);
        active.put(sessionId, state);
        startBeacon(state);
    }

    public void stop(UUID sessionId) {
        ActivePreOpen state = active.remove(sessionId);
        if (state == null) {
            return;
        }
        if (state.beaconTask != null) {
            state.beaconTask.cancel();
        }
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
                player.addPotionEffect(effect);
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
        private BukkitTask beaconTask;

        private ActivePreOpen(UUID sessionId, AirDropTypeDefinition definition, Location anchor, Instant lootableAt) {
            this.sessionId = sessionId;
            this.definition = definition;
            this.anchor = anchor;
            this.lootableAt = lootableAt;
        }
    }
}
