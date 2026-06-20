package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeMobCombatSettings;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class HordeMobTargetingService {

    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private final MobWaveService waveService;
    private BukkitTask task;

    public HordeMobTargetingService(Plugin plugin, MobWavesPluginConfig config, MobWaveService waveService) {
        this.plugin = plugin;
        this.config = config;
        this.waveService = waveService;
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
    }

    private void tick() {
        HordeMobCombatSettings combat = config.module().hordeCombat;
        if (!combat.forceTargetPlayers) {
            return;
        }
        int radius = Math.max(8, combat.targetRadiusBlocks);
        waveService.forEachAliveMob(mob -> retarget(mob, radius));
    }

    private void retarget(LivingEntity entity, int radius) {
        if (!(entity instanceof Mob mob) || mob.isDead() || !mob.isValid()) {
            return;
        }
        LivingEntity current = mob.getTarget();
        if (current instanceof Player player && isValidTarget(player)) {
            return;
        }
        if (current != null && current.isValid() && !current.isDead()) {
            mob.setTarget(null);
        }
        Player nearest = findNearestPlayer(mob, radius);
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    public static void assignNearestPlayer(Mob mob, int radius) {
        if (mob == null || mob.isDead() || !mob.isValid() || mob.getWorld() == null) {
            return;
        }
        int searchRadius = Math.max(8, radius);
        double radiusSquared = (double) searchRadius * searchRadius;
        Player nearest = null;
        double nearestDistance = radiusSquared;
        for (Player player : mob.getWorld().getPlayers()) {
            if (!isValidTarget(player)) {
                continue;
            }
            double distance = player.getLocation().distanceSquared(mob.getLocation());
            if (distance <= nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        if (nearest != null) {
            mob.setTarget(nearest);
        }
    }

    private Player findNearestPlayer(Mob mob, int radius) {
        if (mob.getWorld() == null) {
            return null;
        }
        double radiusSquared = (double) radius * radius;
        Player nearest = null;
        double nearestDistance = radiusSquared;
        for (Player player : mob.getWorld().getPlayers()) {
            if (!isValidTarget(player)) {
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

    private static boolean isValidTarget(Player player) {
        return player.isOnline()
                && !player.isDead()
                && player.getGameMode() != GameMode.SPECTATOR
                && player.getGameMode() != GameMode.CREATIVE;
    }
}
