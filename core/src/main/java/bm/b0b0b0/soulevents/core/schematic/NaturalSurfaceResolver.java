package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;

final class NaturalSurfaceResolver {

    private NaturalSurfaceResolver() {
    }

    static int groundY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
    }

    static boolean isVegetationSurface(Material type) {
        return Tag.LOGS.isTagged(type)
                || Tag.LEAVES.isTagged(type)
                || isClearableObstruction(type);
    }

    static boolean isClearableObstruction(Material type) {
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
                 COCOA, MELON_STEM, ATTACHED_MELON_STEM, PUMPKIN_STEM, ATTACHED_PUMPKIN_STEM -> true;
            default -> false;
        };
    }

    static int clearColumnObstructions(World world, int x, int z, int minY, int maxY) {
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
