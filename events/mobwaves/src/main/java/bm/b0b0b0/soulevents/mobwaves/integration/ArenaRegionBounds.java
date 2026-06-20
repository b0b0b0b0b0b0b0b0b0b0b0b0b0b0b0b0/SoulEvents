package bm.b0b0b0.soulevents.mobwaves.integration;

import org.bukkit.Location;

public record ArenaRegionBounds(
        String worldName,
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {

    public boolean contains(Location location) {
        if (location.getWorld() == null || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }
}

