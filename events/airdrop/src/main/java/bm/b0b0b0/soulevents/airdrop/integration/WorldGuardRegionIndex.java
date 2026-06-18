package bm.b0b0b0.soulevents.airdrop.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WorldGuardRegionIndex {

    private final List<RegionBox> regions;

    private WorldGuardRegionIndex(List<RegionBox> regions) {
        this.regions = List.copyOf(regions);
    }

    public static WorldGuardRegionIndex empty() {
        return new WorldGuardRegionIndex(List.of());
    }

    public static WorldGuardRegionIndex snapshot(World world) {
        if (world == null || Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {
            return empty();
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) {
            return empty();
        }
        List<RegionBox> boxes = new ArrayList<>();
        for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
            if (isIgnored(entry.getKey())) {
                continue;
            }
            ProtectedRegion region = entry.getValue();
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            boxes.add(new RegionBox(min.x(), min.y(), min.z(), max.x(), max.y(), max.z()));
        }
        return new WorldGuardRegionIndex(boxes);
    }

    public boolean isInsideAny(int x, int y, int z) {
        for (RegionBox box : regions) {
            if (box.contains(x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public int nearestDistanceBlocks(int x, int y, int z) {
        int nearest = Integer.MAX_VALUE;
        for (RegionBox box : regions) {
            nearest = Math.min(nearest, box.distanceBlocks(x, y, z));
        }
        return nearest;
    }

    private static boolean isIgnored(String regionId) {
        return regionId != null
                && regionId.toLowerCase(Locale.ROOT).startsWith(WorldGuardConstants.AIRDROP_REGION_PREFIX);
    }

    public record RegionBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        public boolean contains(int x, int y, int z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        public int distanceBlocks(int x, int y, int z) {
            int closestX = clamp(x, minX, maxX);
            int closestY = clamp(y, minY, maxY);
            int closestZ = clamp(z, minZ, maxZ);
            int dx = x - closestX;
            int dy = y - closestY;
            int dz = z - closestZ;
            return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
