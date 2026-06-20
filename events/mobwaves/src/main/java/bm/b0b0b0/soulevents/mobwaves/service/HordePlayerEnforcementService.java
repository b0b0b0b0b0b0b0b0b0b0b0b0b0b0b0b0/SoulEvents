package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPermissions;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeLifecycleSettings;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class HordePlayerEnforcementService {

    private final Plugin plugin;
    private MobWavesPluginConfig config;
    private final MobHordeService hordeService;
    private final MobWaveService waveService;
    private BukkitTask task;

    public HordePlayerEnforcementService(
            Plugin plugin,
            MobWavesPluginConfig config,
            MobHordeService hordeService,
            MobWaveService waveService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.hordeService = hordeService;
        this.waveService = waveService;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    public void start() {
        if (task != null) {
            return;
        }
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(MobWavesPermissions.BYPASS)) {
                continue;
            }
            if (!isInsideActiveHorde(player)) {
                continue;
            }
            stripCreativeAndFlight(player);
        }
    }

    private boolean isInsideActiveHorde(Player player) {
        World world = player.getWorld();
        Location playerLocation = player.getLocation();
        for (MobHordeSessionRegistry.SessionRecord record : hordeService.sessions().snapshot()) {
            if (!record.wavesAttached()) {
                continue;
            }
            Location anchor = record.waveAnchor() != null ? record.waveAnchor() : record.anchor();
            if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(world)) {
                continue;
            }
            int radius = enforcementRadius(record.typeId());
            if (radius <= 0) {
                continue;
            }
            if (horizontalDistanceSquared(playerLocation, anchor) <= (double) radius * radius) {
                return true;
            }
        }
        for (MobWaveService.EnforcementZone zone : waveService.enforcementZones()) {
            Location anchor = zone.anchor();
            if (anchor == null || anchor.getWorld() == null || !anchor.getWorld().equals(world)) {
                continue;
            }
            int radius = zone.radiusBlocks();
            if (radius <= 0) {
                continue;
            }
            if (horizontalDistanceSquared(playerLocation, anchor) <= (double) radius * radius) {
                return true;
            }
        }
        return false;
    }

    private int enforcementRadius(String typeId) {
        HordeTypeDefinition definition = config.type(typeId).orElse(null);
        if (definition == null) {
            return 48;
        }
        HordeLifecycleSettings lifecycle = definition.settings().lifecycle;
        if (lifecycle.requirePlayerRadiusBlocks > 0) {
            return lifecycle.requirePlayerRadiusBlocks;
        }
        if (lifecycle.bossBarRadius > 0) {
            return lifecycle.bossBarRadius;
        }
        return 48;
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static void stripCreativeAndFlight(Player player) {
        GameMode mode = player.getGameMode();
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
            player.setGameMode(GameMode.SURVIVAL);
        }
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
        if (player.isFlying()) {
            player.setFlying(false);
        }
    }
}
