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
        return roughnessIssue(
                world,
                originX,
                originZ,
                SchematicSpawnRoughness.limit(maxSurfaceDelta, terrainAdaptBlocks),
                samples
        );
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
