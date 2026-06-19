package bm.b0b0b0.soulevents.airdrop.service;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

final class SpawnSurfaceFilter {

    private SpawnSurfaceFilter() {
    }

    static String waterSurfaceReason(World world, int x, int z, boolean enabled) {
        if (!enabled) {
            return null;
        }
        int y = world.getHighestBlockYAt(x, z);
        Block surface = world.getBlockAt(x, y, z);
        if (surface.isLiquid()) {
            return "water-surface(" + surface.getType().name() + " y=" + y + ")";
        }
        Material type = surface.getType();
        if (type == Material.KELP || type == Material.KELP_PLANT || type == Material.SEAGRASS || type == Material.TALL_SEAGRASS) {
            return "water-surface(" + type.name() + " y=" + y + ")";
        }
        return null;
    }
}
