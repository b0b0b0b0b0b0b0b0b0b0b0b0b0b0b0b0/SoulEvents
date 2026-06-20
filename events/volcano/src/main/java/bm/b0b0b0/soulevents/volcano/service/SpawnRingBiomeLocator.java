package bm.b0b0b0.soulevents.volcano.service;

import org.bukkit.block.Biome;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BiomeSearchResult;

import java.util.Optional;
import java.util.Random;

final class SpawnRingBiomeLocator {

    private static final int LOCATE_TRIES = 16;

    private static final Biome[] FLAT_BIOMES = {
            Biome.PLAINS,
            Biome.SUNFLOWER_PLAINS,
            Biome.SAVANNA,
            Biome.SNOWY_PLAINS,
    };

    private static final Biome[] INLAND_FALLBACK_BIOMES = {
            Biome.PLAINS,
            Biome.SUNFLOWER_PLAINS,
            Biome.SAVANNA,
            Biome.SNOWY_PLAINS,
    };

    private SpawnRingBiomeLocator() {
    }

    record LandPoint(int x, int z) {
    }

    static Optional<LandPoint> locate(World world, MapSpawnBoundary.Area area, Random random) {
        for (int attempt = 0; attempt < LOCATE_TRIES; attempt++) {
            Optional<LandPoint> candidate = locateOnce(world, area, random);
            if (candidate.isEmpty()) {
                continue;
            }
            LandPoint point = candidate.get();
            if (SpawnSurfaceFilter.isBlockedBiome(world, point.x(), point.z())) {
                continue;
            }
            return candidate;
        }
        return Optional.empty();
    }

    private static Optional<LandPoint> locateOnce(World world, MapSpawnBoundary.Area area, Random random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = area.minRadius() == area.maxRadius()
                ? area.minRadius()
                : area.minRadius() + random.nextDouble() * (area.maxRadius() - area.minRadius());
        int anchorX = area.centerX() + (int) Math.round(Math.cos(angle) * distance);
        int anchorZ = area.centerZ() + (int) Math.round(Math.sin(angle) * distance);
        int searchRadius = Math.min(512, Math.max(192, area.maxRadius() / 8));
        Location origin = new Location(world, anchorX, 64, anchorZ);
        BiomeSearchResult found = world.locateNearestBiome(origin, searchRadius, FLAT_BIOMES);
        if (found == null) {
            found = world.locateNearestBiome(origin, searchRadius, INLAND_FALLBACK_BIOMES);
        }
        if (found == null) {
            return Optional.empty();
        }
        Location location = found.getLocation();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        if (!area.withinSpawnRing(x, z) || !area.containsBlock(x, z)) {
            return Optional.empty();
        }
        return Optional.of(new LandPoint(x, z));
    }
}
