package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SchematicFootprintChunks {

    private SchematicFootprintChunks() {
    }

    public static CompletableFuture<Void> loadForFootprint(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            List<FlatSurfaceOffset> footprint,
            int blockMargin
    ) {
        List<long[]> chunks = chunkCoords(pasteOriginBlockX, pasteOriginBlockZ, footprint, blockMargin);
        if (chunks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[chunks.size()];
        for (int index = 0; index < chunks.size(); index++) {
            long[] chunk = chunks.get(index);
            futures[index] = world.getChunkAtAsync((int) chunk[0], (int) chunk[1], true)
                    .thenApply(ignored -> null);
        }
        return CompletableFuture.allOf(futures);
    }

    public static CompletableFuture<Void> loadForSearchOrigins(
            World world,
            List<FlatSurfaceOffset> footprint,
            int blockMargin,
            List<int[]> blockOrigins
    ) {
        if (blockOrigins == null || blockOrigins.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        java.util.Set<Long> seen = new java.util.HashSet<>();
        List<long[]> chunks = new ArrayList<>();
        for (int[] origin : blockOrigins) {
            if (origin == null || origin.length < 2) {
                continue;
            }
            for (long[] chunk : chunkCoords(origin[0], origin[1], footprint, blockMargin)) {
                long key = (chunk[0] << 32) | (chunk[1] & 0xFFFFFFFFL);
                if (seen.add(key)) {
                    chunks.add(chunk);
                }
            }
        }
        if (chunks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[chunks.size()];
        for (int index = 0; index < chunks.size(); index++) {
            long[] chunk = chunks.get(index);
            futures[index] = world.getChunkAtAsync((int) chunk[0], (int) chunk[1], true)
                    .thenApply(ignored -> null);
        }
        return CompletableFuture.allOf(futures);
    }

    public static CompletableFuture<Void> loadForBlockRadius(
            World world,
            int centerBlockX,
            int centerBlockZ,
            int blockRadius
    ) {
        int radius = Math.max(0, blockRadius);
        int minBlockX = centerBlockX - radius;
        int maxBlockX = centerBlockX + radius;
        int minBlockZ = centerBlockZ - radius;
        int maxBlockZ = centerBlockZ + radius;
        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;
        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;
        List<long[]> chunks = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new long[] {chunkX, chunkZ});
            }
        }
        if (chunks.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = new CompletableFuture[chunks.size()];
        for (int index = 0; index < chunks.size(); index++) {
            long[] chunk = chunks.get(index);
            futures[index] = world.getChunkAtAsync((int) chunk[0], (int) chunk[1], true)
                    .thenApply(ignored -> null);
        }
        return CompletableFuture.allOf(futures);
    }

    public static List<long[]> chunkCoords(
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            List<FlatSurfaceOffset> footprint,
            int blockMargin
    ) {
        if (footprint.isEmpty()) {
            int radius = Math.max(0, blockMargin);
            int minBlockX = pasteOriginBlockX - radius;
            int maxBlockX = pasteOriginBlockX + radius;
            int minBlockZ = pasteOriginBlockZ - radius;
            int maxBlockZ = pasteOriginBlockZ + radius;
            int minChunkX = minBlockX >> 4;
            int maxChunkX = maxBlockX >> 4;
            int minChunkZ = minBlockZ >> 4;
            int maxChunkZ = maxBlockZ >> 4;
            List<long[]> coords = new ArrayList<>();
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    coords.add(new long[] {chunkX, chunkZ});
                }
            }
            return coords;
        }
        SchematicPlacementProbeBuilder.Bounds bounds =
                SchematicPlacementProbeBuilder.Bounds.of(footprint);
        int minBlockX = pasteOriginBlockX + bounds.minDx() - blockMargin;
        int maxBlockX = pasteOriginBlockX + bounds.maxDx() + blockMargin;
        int minBlockZ = pasteOriginBlockZ + bounds.minDz() - blockMargin;
        int maxBlockZ = pasteOriginBlockZ + bounds.maxDz() + blockMargin;
        int minChunkX = minBlockX >> 4;
        int maxChunkX = maxBlockX >> 4;
        int minChunkZ = minBlockZ >> 4;
        int maxChunkZ = maxBlockZ >> 4;
        List<long[]> coords = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                coords.add(new long[] {chunkX, chunkZ});
            }
        }
        return coords;
    }
}
