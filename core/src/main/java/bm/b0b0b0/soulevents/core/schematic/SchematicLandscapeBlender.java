package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SchematicLandscapeBlender {

    private static final Set<Material> BLEND_REPLACEABLE = EnumSet.of(
            Material.STONE,
            Material.COBBLESTONE,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.SAND,
            Material.GRAVEL,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.MOSS_BLOCK
    );

    private SchematicLandscapeBlender() {
    }

    public static int blend(World world, SchematicWorldBounds bounds, int radius) {
        if (radius <= 0) {
            return 0;
        }
        int floorY = bounds.minY();
        int[][] targetHeights = buildTargetHeights(world, bounds, floorY, radius);
        int changed = 0;
        for (int x = bounds.minX() - radius; x <= bounds.maxX() + radius; x++) {
            for (int z = bounds.minZ() - radius; z <= bounds.maxZ() + radius; z++) {
                int arrayX = x - (bounds.minX() - radius);
                int arrayZ = z - (bounds.minZ() - radius);
                int distance = horizontalDistanceToBox(x, z, bounds);
                if (distance <= 0 || distance > radius) {
                    continue;
                }
                changed += sculptColumn(
                        world,
                        x,
                        z,
                        targetHeights[arrayX][arrayZ],
                        distance,
                        radius
                );
            }
        }
        return changed;
    }

    private static int[][] buildTargetHeights(World world, SchematicWorldBounds bounds, int floorY, int radius) {
        int width = bounds.maxX() - bounds.minX() + 1 + radius * 2;
        int depth = bounds.maxZ() - bounds.minZ() + 1 + radius * 2;
        int[][] heights = new int[width][depth];
        for (int x = bounds.minX() - radius; x <= bounds.maxX() + radius; x++) {
            for (int z = bounds.minZ() - radius; z <= bounds.maxZ() + radius; z++) {
                int arrayX = x - (bounds.minX() - radius);
                int arrayZ = z - (bounds.minZ() - radius);
                int distance = horizontalDistanceToBox(x, z, bounds);
                if (distance <= 0) {
                    heights[arrayX][arrayZ] = floorY;
                    continue;
                }
                if (distance > radius) {
                    heights[arrayX][arrayZ] = SchematicTerrainAdapter.highestSolidY(world, x, z);
                    continue;
                }
                double factor = smoothStep(distance / (double) (radius + 1));
                int naturalY = SchematicTerrainAdapter.highestSolidY(world, x, z);
                heights[arrayX][arrayZ] = (int) Math.round(floorY + (naturalY - floorY) * factor);
            }
        }
        return smoothHeightMap(heights, 1);
    }

    private static int[][] smoothHeightMap(int[][] source, int passes) {
        int[][] current = source;
        for (int pass = 0; pass < passes; pass++) {
            int width = current.length;
            int depth = current[0].length;
            int[][] next = new int[width][depth];
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    next[x][z] = averageNeighborhood(current, x, z);
                }
            }
            current = next;
        }
        return current;
    }

    private static int averageNeighborhood(int[][] heights, int x, int z) {
        int sum = heights[x][z];
        int count = 1;
        if (x > 0) {
            sum += heights[x - 1][z];
            count++;
        }
        if (x + 1 < heights.length) {
            sum += heights[x + 1][z];
            count++;
        }
        if (z > 0) {
            sum += heights[x][z - 1];
            count++;
        }
        if (z + 1 < heights[0].length) {
            sum += heights[x][z + 1];
            count++;
        }
        return (int) Math.round(sum / (double) count);
    }

    private static int sculptColumn(
            World world,
            int x,
            int z,
            int targetSurfaceY,
            int distance,
            int radius
    ) {
        int currentSurfaceY = SchematicTerrainAdapter.highestSolidY(world, x, z);
        if (currentSurfaceY <= world.getMinHeight()) {
            return 0;
        }
        Material top = SchematicTerrainAdapter.sampleNaturalTop(world, x, z);
        double factor = 1.0 - (distance / (double) (radius + 1));
        int blendDepth = Math.max(1, (int) Math.ceil(3.0 * factor));
        int changed = 0;

        if (currentSurfaceY > targetSurfaceY) {
            for (int y = currentSurfaceY; y > targetSurfaceY; y--) {
                Block block = world.getBlockAt(x, y, z);
                if (block.getType().isAir()) {
                    continue;
                }
                block.setType(Material.AIR, false);
                changed++;
            }
        } else if (currentSurfaceY < targetSurfaceY) {
            for (int y = currentSurfaceY + 1; y <= targetSurfaceY; y++) {
                Material material = y == targetSurfaceY
                        ? SchematicTerrainAdapter.topMaterial(top)
                        : Material.DIRT;
                Block block = world.getBlockAt(x, y, z);
                if (block.getType() != material) {
                    block.setType(material, false);
                    changed++;
                }
            }
        }

        int surfaceY = targetSurfaceY;
        for (int depth = 0; depth < blendDepth; depth++) {
            Block block = world.getBlockAt(x, surfaceY - depth, z);
            if (!BLEND_REPLACEABLE.contains(block.getType())) {
                continue;
            }
            Material target = depth == 0
                    ? SchematicTerrainAdapter.topMaterial(top)
                    : Material.DIRT;
            if (block.getType() != target) {
                block.setType(target, false);
                changed++;
            }
        }
        return changed;
    }

    static int estimatePreBlendCaptureCount(SchematicDefinition.SchematicMetadata metadata, int radius) {
        if (radius <= 0) {
            return 0;
        }
        int ringCells = 2 * radius * (metadata.sizeX() + metadata.sizeZ() + 2 * radius);
        return ringCells * 8;
    }

    static void appendPreBlendCapture(
            World world,
            SchematicWorldBounds bounds,
            int radius,
            List<WorldEditSchematicBridge.BlockSnapshot> snapshots,
            Set<Long> capturedKeys
    ) {
        if (radius <= 0) {
            return;
        }
        int floorY = bounds.minY();
        int[][] targetHeights = buildTargetHeights(world, bounds, floorY, radius);
        int arrayBaseX = bounds.minX() - radius;
        int arrayBaseZ = bounds.minZ() - radius;
        for (int x = bounds.minX() - radius; x <= bounds.maxX() + radius; x++) {
            for (int z = bounds.minZ() - radius; z <= bounds.maxZ() + radius; z++) {
                int distance = horizontalDistanceToBox(x, z, bounds);
                if (distance <= 0 || distance > radius) {
                    continue;
                }
                int arrayX = x - arrayBaseX;
                int arrayZ = z - arrayBaseZ;
                int targetY = targetHeights[arrayX][arrayZ];
                int naturalY = SchematicTerrainAdapter.highestSolidY(world, x, z);
                int minY = Math.min(naturalY, targetY) - 3;
                int maxY = Math.max(naturalY, targetY);
                for (int y = minY; y <= maxY; y++) {
                    WorldEditSchematicBridge.appendSnapshotIfAbsent(world, snapshots, capturedKeys, x, y, z);
                }
            }
        }
    }

    private static double smoothStep(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static int horizontalDistanceToBox(int x, int z, SchematicWorldBounds bounds) {
        if (x >= bounds.minX() && x <= bounds.maxX() && z >= bounds.minZ() && z <= bounds.maxZ()) {
            return 0;
        }
        int nearX = Math.max(bounds.minX(), Math.min(x, bounds.maxX()));
        int nearZ = Math.max(bounds.minZ(), Math.min(z, bounds.maxZ()));
        int dx = x - nearX;
        int dz = z - nearZ;
        return (int) Math.ceil(Math.sqrt((double) dx * dx + (double) dz * dz));
    }
}
