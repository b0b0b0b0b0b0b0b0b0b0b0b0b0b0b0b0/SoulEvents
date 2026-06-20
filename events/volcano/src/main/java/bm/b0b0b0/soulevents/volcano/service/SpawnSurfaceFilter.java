package bm.b0b0b0.soulevents.volcano.service;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;

final class SpawnSurfaceFilter {

    private static final int WATER_EXCLUSION_DEPTH = 6;

    private SpawnSurfaceFilter() {
    }

    static boolean isBlockedBiome(World world, int x, int z) {
        String key = world.getBiome(x, 64, z).getKey().getKey();
        return isWaterBiomeKey(key);
    }

    static String waterSurfaceReason(World world, int x, int z, boolean enabled) {
        if (!enabled || !isChunkLoaded(world, x, z)) {
            return null;
        }
        String biomeIssue = waterBiomeReason(world, x, z);
        if (biomeIssue != null) {
            return biomeIssue;
        }
        return waterColumnReason(world, x, z);
    }

    private static String waterBiomeReason(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        String key = world.getBiome(x, y, z).getKey().getKey();
        if (isWaterBiomeKey(key)) {
            return "water-biome(" + key + " y=" + y + ")";
        }
        return null;
    }

    private static boolean isWaterBiomeKey(String key) {
        return key.contains("ocean")
                || key.equals("river")
                || key.equals("beach")
                || key.equals("stony_shore")
                || key.equals("snowy_beach")
                || key.contains("swamp")
                || key.equals("mangrove_swamp");
    }

    private static String waterColumnReason(World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z);
        Block surface = world.getBlockAt(x, y, z);
        if (surface.isLiquid() || isWaterlogged(surface)) {
            return "water-surface(" + surface.getType().name() + " y=" + y + ")";
        }
        Material type = surface.getType();
        if (type == Material.KELP
                || type == Material.KELP_PLANT
                || type == Material.SEAGRASS
                || type == Material.TALL_SEAGRASS) {
            return "water-surface(" + type.name() + " y=" + y + ")";
        }
        int minY = Math.max(world.getMinHeight(), y - WATER_EXCLUSION_DEPTH);
        for (int probeY = y - 1; probeY >= minY; probeY--) {
            Block block = world.getBlockAt(x, probeY, z);
            Material below = block.getType();
            if (below == Material.WATER || below == Material.LAVA || isWaterlogged(block)) {
                return "water-nearby(" + below.name() + " at=" + x + "," + probeY + "," + z + ")";
            }
        }
        return null;
    }

    private static boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
    }

    private static boolean isWaterlogged(Block block) {
        if (!(block.getBlockData() instanceof Waterlogged waterlogged)) {
            return false;
        }
        return waterlogged.isWaterlogged();
    }
}
