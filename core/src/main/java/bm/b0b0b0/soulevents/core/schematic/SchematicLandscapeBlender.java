package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;

public final class SchematicLandscapeBlender {

    private static final Set<Material> NATURAL_TOP = EnumSet.of(
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.SAND,
            Material.RED_SAND,
            Material.SNOW_BLOCK,
            Material.MOSS_BLOCK
    );

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
            Material.GRAVEL
    );

    private SchematicLandscapeBlender() {
    }

    public static int blend(World world, SchematicWorldBounds bounds, int radius) {
        if (radius <= 0) {
            return 0;
        }
        int changed = 0;
        for (int x = bounds.minX() - radius; x <= bounds.maxX() + radius; x++) {
            for (int z = bounds.minZ() - radius; z <= bounds.maxZ() + radius; z++) {
                int distance = horizontalDistanceToBox(x, z, bounds);
                if (distance <= 0 || distance > radius) {
                    continue;
                }
                changed += blendColumn(world, x, z, distance, radius);
            }
        }
        return changed;
    }

    private static int blendColumn(World world, int x, int z, int distance, int radius) {
        double factor = 1.0 - (distance / (double) (radius + 1));
        if (factor <= 0.0) {
            return 0;
        }
        Material reference = sampleNaturalTop(world, x, z);
        int surfaceY = highestSolidY(world, x, z);
        if (surfaceY <= world.getMinHeight()) {
            return 0;
        }
        int blendDepth = Math.max(1, (int) Math.ceil(3.0 * factor));
        int changed = 0;
        for (int depth = 0; depth < blendDepth; depth++) {
            Block block = world.getBlockAt(x, surfaceY - depth, z);
            if (!BLEND_REPLACEABLE.contains(block.getType())) {
                continue;
            }
            Material target = depth == 0 ? topMaterial(reference) : Material.DIRT;
            if (block.getType() != target) {
                block.setType(target, false);
                changed++;
            }
        }
        return changed;
    }

    private static Material sampleNaturalTop(World world, int x, int z) {
        for (int ox = -4; ox <= 4; ox++) {
            for (int oz = -4; oz <= 4; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int y = highestSolidY(world, x + ox, z + oz);
                Material material = world.getBlockAt(x + ox, y, z + oz).getType();
                if (NATURAL_TOP.contains(material)) {
                    return material;
                }
            }
        }
        return Material.GRASS_BLOCK;
    }

    private static Material topMaterial(Material reference) {
        if (reference == Material.DIRT || reference == Material.COARSE_DIRT || reference == Material.ROOTED_DIRT) {
            return Material.GRASS_BLOCK;
        }
        return reference;
    }

    private static int highestSolidY(World world, int x, int z) {
        int maxY = world.getMaxHeight() - 1;
        for (int y = maxY; y >= world.getMinHeight(); y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (!type.isAir() && type != Material.WATER && type != Material.LAVA) {
                return y;
            }
        }
        return world.getMinHeight();
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
