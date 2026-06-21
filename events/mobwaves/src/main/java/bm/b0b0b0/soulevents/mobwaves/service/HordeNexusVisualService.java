package bm.b0b0b0.soulevents.mobwaves.service;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class HordeNexusVisualService {

    private final Plugin plugin;
    private final Map<UUID, BukkitTask> pulseTasks = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> finaleTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> anchors = new ConcurrentHashMap<>();

    public HordeNexusVisualService(Plugin plugin) {
        this.plugin = plugin;
    }

    public void start(UUID sessionId, Location anchor) {
        stop(sessionId);
        Location copy = anchor.clone();
        anchors.put(sessionId, copy);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> pulse(sessionId, copy),
                0L,
                8L
        );
        pulseTasks.put(sessionId, task);
    }

    public void stop(UUID sessionId) {
        BukkitTask pulse = pulseTasks.remove(sessionId);
        if (pulse != null) {
            pulse.cancel();
        }
        BukkitTask finale = finaleTasks.remove(sessionId);
        if (finale != null) {
            finale.cancel();
        }
        anchors.remove(sessionId);
    }

    public void shutdown() {
        pulseTasks.values().forEach(BukkitTask::cancel);
        pulseTasks.clear();
        finaleTasks.values().forEach(BukkitTask::cancel);
        finaleTasks.clear();
        anchors.clear();
    }

    public void playBossSlainThunder(Location nexus, Location strikeHint) {
        World world = nexus.getWorld();
        if (world == null) {
            return;
        }
        Location center = nexusCenter(nexus);
        Location strike = strikeHint != null && strikeHint.getWorld() != null && strikeHint.getWorld().equals(world)
                ? strikeHint.clone().add(0.0, 0.5, 0.0)
                : center;
        world.strikeLightningEffect(strike);
        world.strikeLightningEffect(center);
        playRollingThunder(world, center, 48, 1.05f, 0.58f);
        spawn(world, Particle.FLASH, center, 2, 0.15, 0.2, 0.15, 0.0);
        spawn(world, Particle.SOUL_FIRE_FLAME, center, 24, 0.55, 0.75, 0.55, 0.04);
        spawn(world, Particle.REVERSE_PORTAL, center, 40, 0.65, 1.0, 0.65, 0.08);
    }

    public void playRiftRecallLightning(Location nexus, Location mobLocation) {
        World world = nexus.getWorld();
        if (world == null || mobLocation.getWorld() == null || !mobLocation.getWorld().equals(world)) {
            return;
        }
        Location center = nexusCenter(nexus);
        Location mob = mobLocation.clone().add(0.0, 1.0, 0.0);
        world.strikeLightningEffect(mob);
        world.strikeLightningEffect(center);
        drawRecallBeam(world, mob, center);
        playRollingThunder(world, center, 32, 0.75f, 0.62f);
        world.spawnParticle(Particle.PORTAL, mob, 20, 0.35, 0.5, 0.35, 0.6);
        world.spawnParticle(Particle.WITCH, center, 12, 0.4, 0.55, 0.4, 0.02);
    }

    public void beginVictoryFinale(UUID sessionId, Location anchor, int durationSeconds) {
        BukkitTask pulse = pulseTasks.remove(sessionId);
        if (pulse != null) {
            pulse.cancel();
        }
        BukkitTask existing = finaleTasks.remove(sessionId);
        if (existing != null) {
            existing.cancel();
        }
        Location copy = anchor.clone();
        anchors.put(sessionId, copy);
        int maxTicks = Math.max(20, durationSeconds * 20);
        int[] tick = {0};
        BukkitTask finale = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                () -> {
                    if (tick[0] >= maxTicks) {
                        stop(sessionId);
                        return;
                    }
                    victoryPulse(copy, tick[0], maxTicks);
                    tick[0] += 4;
                },
                0L,
                4L
        );
        finaleTasks.put(sessionId, finale);
        playVictoryOpening(copy);
    }

    private void pulse(UUID sessionId, Location anchor) {
        if (!pulseTasks.containsKey(sessionId)) {
            return;
        }
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        Location center = nexusCenter(anchor);
        world.spawnParticle(Particle.PORTAL, center, 16, 0.45, 0.6, 0.45, 0.02);
        spawn(world, Particle.SOUL_FIRE_FLAME, center, 6, 0.25, 0.35, 0.25, 0.01);
        if (ThreadLocalRandom.current().nextInt(4) == 0) {
            world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 0.35f, 0.7f + ThreadLocalRandom.current().nextFloat() * 0.3f);
        }
    }

    private void victoryPulse(Location anchor, int tick, int maxTicks) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        Location center = nexusCenter(anchor);
        float progress = Math.min(1F, tick / (float) maxTicks);
        float intensity = 1F - progress * 0.55F;
        double radius = 0.35 + progress * 2.4;
        int burst = Math.max(8, (int) (48 * intensity));
        spawn(world, Particle.REVERSE_PORTAL, center, burst, radius, 1.1 + progress, radius, 0.12);
        spawn(world, Particle.END_ROD, center, Math.max(4, burst / 3), radius * 0.8, 1.4, radius * 0.8, 0.03);
        spawn(world, Particle.SOUL_FIRE_FLAME, center, Math.max(6, burst / 2), radius * 0.6, 0.9, radius * 0.6, 0.02);
        spawn(world, Particle.DRAGON_BREATH, center, Math.max(4, burst / 4), radius * 0.5, 0.35, radius * 0.5, 0.01);
        if (tick == 0) {
            spawn(world, Particle.EXPLOSION_EMITTER, center, 2, 0.2, 0.2, 0.2, 0.0);
        }
        if (tick % 24 == 0) {
            world.playSound(center, Sound.BLOCK_BEACON_POWER_SELECT, 0.9f, 0.55f + progress * 0.35f);
        }
        if (tick % 40 == 0) {
            playRollingThunder(world, center, 56, 0.55f + intensity * 0.35f, 0.48f + progress * 0.2f);
        }
        if (tick % 16 == 0) {
            world.playSound(center, Sound.BLOCK_PORTAL_TRAVEL, 0.45f, 0.35f + progress * 0.25f);
        }
    }

    private void playVictoryOpening(Location anchor) {
        World world = anchor.getWorld();
        if (world == null) {
            return;
        }
        Location center = nexusCenter(anchor);
        world.strikeLightningEffect(center);
        spawn(world, Particle.FLASH, center, 3, 0.25, 0.35, 0.25, 0.0);
        spawn(world, Particle.TOTEM_OF_UNDYING, center, 80, 1.2, 1.6, 1.2, 0.25);
        spawn(world, Particle.FIREWORK, center, 40, 1.0, 1.4, 1.0, 0.08);
        playRollingThunder(world, center, 64, 1.25f, 0.52f);
        world.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 0.85f);
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.55f, 0.65f);
    }

    private static void drawRecallBeam(World world, Location from, Location to) {
        Vector delta = to.toVector().subtract(from.toVector());
        double length = delta.length();
        if (length < 0.5) {
            return;
        }
        Vector step = delta.multiply(1.0 / length);
        int points = Math.min(24, Math.max(6, (int) length));
        Location cursor = from.clone();
        for (int index = 0; index <= points; index++) {
            world.spawnParticle(Particle.WITCH, cursor, 2, 0.02, 0.02, 0.02, 0.0);
            world.spawnParticle(Particle.PORTAL, cursor, 1, 0.0, 0.0, 0.0, 0.15);
            cursor.add(step.clone().multiply(length / points));
        }
    }

    private static void playRollingThunder(World world, Location center, int radiusBlocks, float volume, float pitch) {
        double radiusSquared = (double) radiusBlocks * radiusBlocks;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                player.playSound(center, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, volume, pitch);
            }
        }
    }

    private static void spawn(
            World world,
            Particle particle,
            Location location,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double extra
    ) {
        Class<?> dataType = particle.getDataType();
        if (dataType == Void.class) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
            return;
        }
        if (dataType == Color.class) {
            world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, Color.WHITE);
            return;
        }
        world.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
    }

    private static Location nexusCenter(Location anchor) {
        return anchor.clone().add(0.5, 0.5, 0.5);
    }
}
