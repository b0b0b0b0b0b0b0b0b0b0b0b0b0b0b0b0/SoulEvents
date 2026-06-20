package bm.b0b0b0.soulevents.mobwaves.integration;

import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

public final class WorldGuardIntegrations {

    private WorldGuardIntegrations() {
    }

    public static boolean present() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    public static WorldGuardProbe createProbe() {
        if (!present()) {
            return new NoOpWorldGuardProbe();
        }
        return new WorldGuardProbeImpl();
    }

    public static ArenaRegionService createArenaRegionService(Plugin plugin) {
        if (!present()) {
            return new NoOpArenaRegionService();
        }
        return new HordeArenaRegionService(plugin);
    }

    public static WorldGuardRegionIndex regionIndex(World world) {
        if (!present()) {
            return WorldGuardRegionIndex.empty();
        }
        return WorldGuardRegionIndex.snapshot(world);
    }
}
