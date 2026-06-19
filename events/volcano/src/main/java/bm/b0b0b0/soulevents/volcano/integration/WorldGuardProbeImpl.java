package bm.b0b0b0.soulevents.volcano.integration;

import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class WorldGuardProbeImpl implements WorldGuardProbe {

    public static final String volcano_REGION_PREFIX = WorldGuardConstants.volcano_REGION_PREFIX;

    @Override
    public boolean available() {
        return Bukkit.getPluginManager().getPlugin("WorldGuard") != null;
    }

    @Override
    public Set<String> ignoredRegionIdPrefixes() {
        return Set.of(volcano_REGION_PREFIX);
    }

    @Override
    public Set<String> regionsAt(Location location) {
        Set<String> names = new LinkedHashSet<>();
        if (!available() || location.getWorld() == null) {
            return names;
        }
        RegionManager manager = manager(location);
        if (manager == null) {
            return names;
        }
        BlockVector3 vector = BukkitAdapter.asBlockVector(location);
        ApplicableRegionSet regions = manager.getApplicableRegions(vector);
        for (ProtectedRegion region : regions) {
            if (!isIgnored(region.getId())) {
                names.add(region.getId());
            }
        }
        return names;
    }

    @Override
    public boolean isInsideAnyRegion(Location location, Set<String> ignoredRegionIds) {
        if (!available() || location.getWorld() == null) {
            return false;
        }
        RegionManager manager = manager(location);
        if (manager == null) {
            return false;
        }
        BlockVector3 vector = BukkitAdapter.asBlockVector(location);
        for (ProtectedRegion region : manager.getApplicableRegions(vector)) {
            if (shouldIgnore(region.getId(), ignoredRegionIds)) {
                continue;
            }
            if (region.contains(vector)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int nearestRegionDistanceBlocks(Location location, Set<String> ignoredRegionIds) {
        if (!available() || location.getWorld() == null) {
            return Integer.MAX_VALUE;
        }
        RegionManager manager = manager(location);
        if (manager == null) {
            return Integer.MAX_VALUE;
        }
        BlockVector3 point = BukkitAdapter.asBlockVector(location);
        int nearest = Integer.MAX_VALUE;
        for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
            if (shouldIgnore(entry.getKey(), ignoredRegionIds)) {
                continue;
            }
            nearest = Math.min(nearest, distanceToRegionBlocks(point, entry.getValue()));
        }
        return nearest;
    }

    private RegionManager manager(Location location) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(location.getWorld()));
    }

    private static int distanceToRegionBlocks(BlockVector3 point, ProtectedRegion region) {
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int closestX = clamp(point.x(), min.x(), max.x());
        int closestY = clamp(point.y(), min.y(), max.y());
        int closestZ = clamp(point.z(), min.z(), max.z());
        BlockVector3 closest = BlockVector3.at(closestX, closestY, closestZ);
        return (int) Math.round(Math.sqrt(point.distanceSq(closest)));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isIgnored(String regionId) {
        if (regionId == null) {
            return true;
        }
        String lower = regionId.toLowerCase(Locale.ROOT);
        return lower.startsWith(volcano_REGION_PREFIX);
    }

    private static boolean shouldIgnore(String regionId, Set<String> ignoredRegionIds) {
        if (regionId == null) {
            return true;
        }
        String lower = regionId.toLowerCase(Locale.ROOT);
        if (lower.startsWith(volcano_REGION_PREFIX)) {
            return true;
        }
        if (ignoredRegionIds == null || ignoredRegionIds.isEmpty()) {
            return false;
        }
        for (String ignored : ignoredRegionIds) {
            if (ignored != null && ignored.equalsIgnoreCase(regionId)) {
                return true;
            }
        }
        return false;
    }
}

