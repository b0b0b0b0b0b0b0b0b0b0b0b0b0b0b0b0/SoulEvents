package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobCombatSettings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class HordeMobTargetingService {

    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private final MobWaveService waveService;
    private final HordeNexusVisualService nexusVisualService;
    private BukkitTask task;
    private final Map<UUID, Integer> outOfRangeAttempts = new ConcurrentHashMap<>();

    public HordeMobTargetingService(
            Plugin plugin,
            MobWavesPluginConfig config,
            MobWaveService waveService,
            HordeNexusVisualService nexusVisualService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.waveService = waveService;
        this.nexusVisualService = nexusVisualService;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        outOfRangeAttempts.clear();
    }

    private void tick() {
        HordeMobCombatSettings combat = config.module().hordeCombat;
        if (!combat.forceTargetPlayers && !combat.keepNearAnchor) {
            return;
        }
        waveService.forEachAliveMob((mob, anchorContext) -> {
            if (combat.keepNearAnchor) {
                enforceAnchorLeash(mob, anchorContext, combat);
            }
            if (combat.forceTargetPlayers) {
                retarget(mob, Math.max(8, combat.targetRadiusBlocks), anchorContext);
            }
        });
    }

    private void enforceAnchorLeash(
            LivingEntity entity,
            MobWaveService.MobAnchorContext anchorContext,
            HordeMobCombatSettings combat
    ) {
        if (!(entity instanceof Mob mob) || mob.isDead() || !mob.isValid()) {
            outOfRangeAttempts.remove(entity.getUniqueId());
            return;
        }
        Location anchor = anchorContext.anchor();
        if (anchor.getWorld() == null || mob.getWorld() == null || !mob.getWorld().equals(anchor.getWorld())) {
            return;
        }
        double pullRadiusSquared = (double) anchorContext.pullBackRadiusBlocks() * anchorContext.pullBackRadiusBlocks();
        if (horizontalDistanceSquared(mob.getLocation(), anchor) <= pullRadiusSquared) {
            outOfRangeAttempts.remove(mob.getUniqueId());
            return;
        }
        Location destination = pullDestination(anchor);
        int attempts = outOfRangeAttempts.merge(mob.getUniqueId(), 1, Integer::sum);
        mob.setTarget(null);
        pullToward(mob, destination, combat);
        if (combat.mobPullParticlesEnabled && attempts % 2 == 0) {
            spawnPullParticles(mob.getLocation(), destination);
        }
        int recallLimit = Math.max(2, combat.mobRecallAttemptsBeforeLightning);
        if (attempts < recallLimit) {
            return;
        }
        nexusVisualService.playRiftRecallLightning(anchor, mob.getLocation());
        mob.teleport(destination);
        mob.setVelocity(new Vector(0, 0, 0));
        outOfRangeAttempts.remove(mob.getUniqueId());
    }

    private void pullToward(Mob mob, Location destination, HordeMobCombatSettings combat) {
        double speed = Math.max(0.5, combat.mobPullSpeed);
        mob.getPathfinder().moveTo(destination, speed);
        Vector delta = destination.toVector().subtract(mob.getLocation().toVector());
        delta.setY(Math.max(-0.15, Math.min(0.35, delta.getY())));
        if (delta.lengthSquared() > 0.01) {
            Vector nudge = delta.normalize().multiply(0.28);
            mob.setVelocity(mob.getVelocity().add(nudge));
        }
    }

    private static void spawnPullParticles(Location from, Location to) {
        World world = from.getWorld();
        if (world == null) {
            return;
        }
        Location midpoint = from.clone().add(to.clone().subtract(from).multiply(0.5));
        world.spawnParticle(Particle.SOUL, midpoint, 3, 0.12, 0.18, 0.12, 0.01);
        world.spawnParticle(Particle.PORTAL, from, 4, 0.15, 0.25, 0.15, 0.08);
    }

    private static Location pullDestination(Location anchor) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return anchor.clone().add(
                random.nextDouble(-2.0, 2.0),
                0.0,
                random.nextDouble(-2.0, 2.0)
        );
    }

    private void retarget(LivingEntity entity, int radius, MobWaveService.MobAnchorContext anchorContext) {
        if (!(entity instanceof Mob mob) || mob.isDead() || !mob.isValid()) {
            return;
        }
        LivingEntity current = mob.getTarget();
        if (current instanceof Player player) {
            if (isValidTarget(player, anchorContext)) {
                return;
            }
            mob.setTarget(null);
        } else if (current != null && current.isValid() && !current.isDead()) {
            mob.setTarget(null);
        }
        Player nearest = findNearestPlayer(mob, radius, anchorContext);
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    public static void assignNearestPlayer(Mob mob, int radius, MobWaveService.MobAnchorContext anchorContext) {
        if (mob == null || mob.isDead() || !mob.isValid() || mob.getWorld() == null) {
            return;
        }
        Player nearest = findNearestPlayer(mob, Math.max(8, radius), anchorContext);
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private static Player findNearestPlayer(Mob mob, int radius, MobWaveService.MobAnchorContext anchorContext) {
        World world = mob.getWorld();
        if (world == null) {
            return null;
        }
        double radiusSquared = (double) radius * radius;
        Player nearest = null;
        double nearestDistance = radiusSquared;
        for (Player player : world.getPlayers()) {
            if (!isValidTarget(player, anchorContext)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static boolean isValidTarget(Player player, MobWaveService.MobAnchorContext anchorContext) {
        if (!player.isOnline()
                || player.isDead()
                || player.getGameMode() == GameMode.SPECTATOR
                || player.getGameMode() == GameMode.CREATIVE) {
            return false;
        }
        Location anchor = anchorContext.anchor();
        if (anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
            return false;
        }
        double chaseRadiusSquared = (double) anchorContext.maxChaseRadiusBlocks() * anchorContext.maxChaseRadiusBlocks();
        return horizontalDistanceSquared(player.getLocation(), anchor) <= chaseRadiusSquared;
    }

    private static double horizontalDistanceSquared(Location left, Location right) {
        double deltaX = left.getX() - right.getX();
        double deltaZ = left.getZ() - right.getZ();
        return deltaX * deltaX + deltaZ * deltaZ;
    }
}
