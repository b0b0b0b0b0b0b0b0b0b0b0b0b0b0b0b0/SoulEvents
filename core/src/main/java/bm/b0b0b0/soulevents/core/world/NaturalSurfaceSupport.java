package bm.b0b0b0.soulevents.core.world;

import bm.b0b0b0.soulevents.core.schematic.NaturalSurfaceResolver;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class NaturalSurfaceSupport {

    private NaturalSurfaceSupport() {
    }

    public static int naturalGroundY(World world, int blockX, int blockZ) {
        return NaturalSurfaceResolver.placementGroundY(world, blockX, blockZ);
    }

    public static int clearObstructions(World world, int blockX, int blockZ, int minY, int maxY) {
        return NaturalSurfaceResolver.clearColumnObstructions(world, blockX, blockZ, minY, maxY);
    }

    public static boolean isValidGroundSurface(Block block) {
        if (block.isLiquid() || !block.isSolid()) {
            return false;
        }
        Material type = block.getType();
        if (type == Material.MAGMA_BLOCK
                || type == Material.CACTUS
                || type == Material.SWEET_BERRY_BUSH
                || type == Material.POWDER_SNOW) {
            return false;
        }
        return !NaturalSurfaceResolver.isVegetationSurface(type);
    }

    public static boolean isBlockingAbove(Block block) {
        if (block.isPassable() || block.isEmpty()) {
            return false;
        }
        return !NaturalSurfaceResolver.isClearableObstruction(block.getType());
    }
}