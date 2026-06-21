package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class NaturalSurfaceResolver {

    private NaturalSurfaceResolver() {
    }

    static int groundY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
    }

    static int spawnSurfaceY(World world, int x, int z) {
        return placementGroundY(world, x, z);
    }

    public static int placementGroundY(World world, int x, int z) {
        int y = groundY(world, x, z);
        int minY = world.getMinHeight();
        while (y >= minY) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (!type.isAir() && !isClearableObstruction(type)) {
                return y;
            }
            y--;
        }
        return minY;
    }

    static void clearFootingColumn(World world, int x, int z, int clearTop) {
        int ground = placementGroundY(world, x, z);
        clearColumnObstructions(world, x, z, ground + 1, clearTop);
    }

    public static boolean isVegetationSurface(Material type) {
        return Tag.LOGS.isTagged(type)
                || Tag.LEAVES.isTagged(type)
                || isClearableObstruction(type);
    }

    public static boolean isClearableObstruction(Material type) {
        if (type.isAir() || type == Material.WATER || type == Material.LAVA) {
            return false;
        }
        if (Tag.LOGS.isTagged(type) || Tag.LEAVES.isTagged(type)) {
            return true;
        }
        if (Tag.FLOWERS.isTagged(type) || Tag.SAPLINGS.isTagged(type)) {
            return true;
        }
        return switch (type) {
            case VINE, GLOW_LICHEN, MANGROVE_ROOTS, MANGROVE_PROPAGULE,
                 BAMBOO, BAMBOO_SAPLING, CHORUS_PLANT, CHORUS_FLOWER,
                 TALL_GRASS, SHORT_GRASS, FERN, LARGE_FERN,
                 DEAD_BUSH, SWEET_BERRY_BUSH, CACTUS, SUGAR_CANE,
                 BIG_DRIPLEAF, BIG_DRIPLEAF_STEM, SMALL_DRIPLEAF,
                 CAVE_VINES, CAVE_VINES_PLANT, TWISTING_VINES, TWISTING_VINES_PLANT,
                 WEEPING_VINES, WEEPING_VINES_PLANT, PITCHER_PLANT, PITCHER_CROP,
                 SPORE_BLOSSOM, HANGING_ROOTS, MOSS_CARPET, AZALEA, FLOWERING_AZALEA,
                 COCOA, MELON_STEM, ATTACHED_MELON_STEM, PUMPKIN_STEM, ATTACHED_PUMPKIN_STEM,
                 LEAF_LITTER, WILDFLOWERS, FIREFLY_BUSH, BUSH, CACTUS_FLOWER, PINK_PETALS -> true;
            default -> false;
        };
    }

    public static int clearColumnObstructions(World world, int x, int z, int minY, int maxY) {
        int cleared = 0;
        int fromY = Math.min(minY, maxY);
        int toY = Math.max(minY, maxY);
        for (int y = fromY; y <= toY; y++) {
            Block block = world.getBlockAt(x, y, z);
            if (isClearableObstruction(block.getType())) {
                block.setType(Material.AIR, false);
                cleared++;
            }
        }
        return cleared;
    }
}
