package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnRoughness;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

final class SpawnQuickCheck {

    private static final int MIN_SOLID_SAMPLES = 3;

    private static final int[][] CARDINAL = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private SpawnQuickCheck() {
    }

    static String rejectCenter(
            World world,
            int originX,
            int originZ,
            boolean skipWaterBiomes
    ) {
        if (!skipWaterBiomes) {
            return null;
        }
        return SpawnSurfaceFilter.waterSurfaceReason(world, originX, originZ, true);
    }

    static String reject(
            World world,
            int originX,
            int originZ,
            boolean skipWaterBiomes,
            int maxSurfaceDelta,
            int terrainAdaptBlocks,
            int minCliffClearanceFromEdge,
            int maxCliffDropFromEdge,
            SchematicService schematics,
            String schematicId
    ) {
        if (skipWaterBiomes) {
            String center = SpawnSurfaceFilter.waterSurfaceReason(world, originX, originZ, true);
            if (center != null) {
                return center;
            }
        }
        List<FlatSurfaceOffset> samples = schematics.perimeterFootprintSamples(schematicId);
        if (skipWaterBiomes) {
            String perimeterWater = perimeterWaterIssue(world, originX, originZ, samples);
            if (perimeterWater != null) {
                return perimeterWater;
            }
        }
        int roughnessLimit = SchematicSpawnRoughness.limit(maxSurfaceDelta, terrainAdaptBlocks);
        String roughness = roughnessIssue(world, originX, originZ, roughnessLimit, samples);
        if (roughness != null) {
            return roughness;
        }
        return cliffBandIssue(
                world,
                originX,
                originZ,
                minCliffClearanceFromEdge,
                effectiveCliffLimit(maxCliffDropFromEdge, terrainAdaptBlocks)
        );
    }

    private static int effectiveCliffLimit(int maxCliffDropFromEdge, int terrainAdaptBlocks) {
        int configured = Math.max(1, maxCliffDropFromEdge);
        int adapt = Math.max(0, terrainAdaptBlocks);
        if (adapt <= 0) {
            return configured;
        }
        return Math.max(configured, adapt);
    }

    private static String cliffBandIssue(
            World world,
            int originX,
            int originZ,
            int cliffBand,
            int cliffLimit
    ) {
        if (cliffBand <= 0) {
            return null;
        }
        int centerY = SpawnGroundHeights.spawnSurfaceY(world, originX, originZ);
        for (int[] direction : CARDINAL) {
            int prevY = centerY;
            for (int distance = 1; distance <= cliffBand; distance++) {
                int x = originX + direction[0] * distance;
                int z = originZ + direction[1] * distance;
                int surfaceY = SpawnGroundHeights.spawnSurfaceY(world, x, z);
                int delta = Math.abs(surfaceY - prevY);
                if (delta > cliffLimit) {
                    return "quick-cliff-near-edge dist=" + distance
                            + " delta=" + delta
                            + " limit=" + cliffLimit
                            + " at=" + x + "," + surfaceY + "," + z;
                }
                prevY = surfaceY;
            }
        }
        return null;
    }

    private static String perimeterWaterIssue(
            World world,
            int originX,
            int originZ,
            List<FlatSurfaceOffset> samples
    ) {
        for (FlatSurfaceOffset offset : samples) {
            int x = originX + offset.dx();
            int z = originZ + offset.dz();
            String issue = SpawnSurfaceFilter.waterSurfaceReason(world, x, z, true);
            if (issue != null) {
                return "perimeter-" + issue;
            }
        }
        return null;
    }

    private static String roughnessIssue(
            World world,
            int originX,
            int originZ,
            int roughnessLimit,
            List<FlatSurfaceOffset> samples
    ) {
        List<Integer> heights = new ArrayList<>(samples.size());
        for (FlatSurfaceOffset offset : samples) {
            int x = originX + offset.dx();
            int z = originZ + offset.dz();
            if (!SpawnGroundHeights.isSolidSurface(world, x, z)) {
                Block surface = world.getBlockAt(x, SpawnGroundHeights.spawnSurfaceY(world, x, z), z);
                return "quick-liquid-perimeter block=" + surface.getType().name()
                        + " at=" + x + "," + surface.getY() + "," + z;
            }
            heights.add(SpawnGroundHeights.spawnSurfaceY(world, x, z));
        }
        if (heights.size() < MIN_SOLID_SAMPLES) {
            if (heights.isEmpty()) {
                return "quick-too-few-solid-samples count=0";
            }
            return null;
        }
        int referenceY = heights.getFirst();
        for (int height : heights) {
            referenceY = Math.max(referenceY, height);
        }
        int limit = Math.max(0, roughnessLimit);
        for (int height : heights) {
            if (referenceY - height > limit) {
                return "quick-terrain-too-rough delta=" + (referenceY - height) + " limit=" + limit;
            }
        }
        return null;
    }
}
