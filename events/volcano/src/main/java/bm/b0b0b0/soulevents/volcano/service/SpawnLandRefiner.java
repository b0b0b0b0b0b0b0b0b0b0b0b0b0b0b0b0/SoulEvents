package bm.b0b0b0.soulevents.volcano.service;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class SpawnLandRefiner {

    private SpawnLandRefiner() {
    }

    static List<SpawnRingBiomeLocator.LandPoint> probeOrigins(
            int anchorX,
            int anchorZ,
            MapSpawnBoundary.Area area,
            Random random,
            int samples,
            int radius
    ) {
        int count = Math.max(1, samples);
        int probeRadius = Math.max(0, radius);
        List<SpawnRingBiomeLocator.LandPoint> points = new ArrayList<>(count);
        points.add(new SpawnRingBiomeLocator.LandPoint(anchorX, anchorZ));
        for (int index = 1; index < count && probeRadius > 0; index++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = random.nextDouble() * probeRadius;
            int x = anchorX + (int) Math.round(Math.cos(angle) * distance);
            int z = anchorZ + (int) Math.round(Math.sin(angle) * distance);
            if (!area.withinSpawnRing(x, z) || !area.containsBlock(x, z)) {
                continue;
            }
            points.add(new SpawnRingBiomeLocator.LandPoint(x, z));
        }
        return List.copyOf(points);
    }

    static String loadedPrefilterReason(World world, int x, int z, boolean skipWaterBiomes) {
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return null;
        }
        return SpawnSurfaceFilter.waterSurfaceReason(world, x, z, skipWaterBiomes);
    }
}
