package bm.b0b0b0.soulevents.volcano.service;

import org.bukkit.HeightMap;
import org.bukkit.World;
import org.bukkit.block.Block;

final class SpawnGroundHeights {

    private SpawnGroundHeights() {
    }

    static int spawnSurfaceY(World world, int x, int z) {
        return world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
    }

    static boolean isSolidSurface(World world, int x, int z) {
        int y = spawnSurfaceY(world, x, z);
        Block block = world.getBlockAt(x, y, z);
        return !block.isLiquid() && block.isSolid();
    }
}
