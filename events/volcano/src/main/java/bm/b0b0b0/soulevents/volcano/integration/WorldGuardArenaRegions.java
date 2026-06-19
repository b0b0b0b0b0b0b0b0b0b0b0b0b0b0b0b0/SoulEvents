package bm.b0b0b0.soulevents.volcano.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public final class WorldGuardArenaRegions {

    private WorldGuardArenaRegions() {
    }

    public static boolean containsTempRegion(Location location) {
        if (location.getWorld() == null || !WorldGuardIntegrations.present()) {
            return false;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(location.getWorld()));
        if (manager == null) {
            return false;
        }
        BlockVector3 vector = BlockVector3.at(
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
        );
        ApplicableRegionSet regions = manager.getApplicableRegions(vector);
        for (ProtectedRegion region : regions) {
            if (isTempRegionId(region.getId())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isTempRegionId(String regionId) {
        return regionId != null
                && regionId.startsWith(WorldGuardConstants.volcano_REGION_PREFIX);
    }
}

