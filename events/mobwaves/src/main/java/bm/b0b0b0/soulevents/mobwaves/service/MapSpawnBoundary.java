package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeRandomSpawnSettings;
import org.bukkit.World;

public final class MapSpawnBoundary {

    private MapSpawnBoundary() {
    }

    public record Area(int centerX, int centerZ, int minRadius, int maxRadius, int boundaryRadius) {

        public boolean hasBoundary() {
            return boundaryRadius > 0;
        }

        public boolean containsBlock(int x, int z) {
            if (!hasBoundary()) {
                return true;
            }
            long dx = (long) x - centerX;
            long dz = (long) z - centerZ;
            long boundarySquared = (long) boundaryRadius * boundaryRadius;
            return dx * dx + dz * dz <= boundarySquared;
        }
    }

    public static Area resolve(HordeRandomSpawnSettings spawn, World world) {
        int centerX = spawn.useWorldSpawnAsCenter ? world.getSpawnLocation().getBlockX() : spawn.centerX;
        int centerZ = spawn.useWorldSpawnAsCenter ? world.getSpawnLocation().getBlockZ() : spawn.centerZ;
        int minRadius = Math.max(0, spawn.minRadiusFromCenter);
        int maxRadius = Math.max(minRadius, spawn.maxRadiusFromCenter);
        int boundaryRadius = 0;
        if (spawn.mapBoundaryEnabled && spawn.mapBoundaryRadius > 0) {
            boundaryRadius = Math.max(0, spawn.mapBoundaryRadius - Math.max(0, spawn.mapBoundaryMargin));
            maxRadius = Math.min(maxRadius, boundaryRadius);
            minRadius = Math.min(minRadius, maxRadius);
        }
        return new Area(centerX, centerZ, minRadius, maxRadius, boundaryRadius);
    }
}
