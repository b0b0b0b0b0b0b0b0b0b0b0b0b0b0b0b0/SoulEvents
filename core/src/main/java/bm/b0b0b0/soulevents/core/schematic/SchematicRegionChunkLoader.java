package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SchematicRegionChunkLoader {

    private static final int EXTRA_BLOCK_MARGIN = 16;

    private SchematicRegionChunkLoader() {
    }

    public static CompletableFuture<Void> ensureLoaded(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin
    ) {
        List<CompletableFuture<Chunk>> futures = new ArrayList<>();
        for (int[] chunk : chunkCoordinates(pasteOrigin, metadata, horizontalMargin)) {
            int chunkX = chunk[0];
            int chunkZ = chunk[1];
            if (world.isChunkLoaded(chunkX, chunkZ)) {
                futures.add(CompletableFuture.completedFuture(world.getChunkAt(chunkX, chunkZ)));
            } else {
                futures.add(world.getChunkAtAsync(chunkX, chunkZ, true));
            }
        }
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    public static void refreshForPlayers(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin
    ) {
        for (int[] chunk : chunkCoordinates(pasteOrigin, metadata, horizontalMargin)) {
            world.refreshChunk(chunk[0], chunk[1]);
        }
    }

    private static List<int[]> chunkCoordinates(
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin
    ) {
        int margin = Math.max(0, horizontalMargin) + EXTRA_BLOCK_MARGIN;
        int minX = pasteOrigin.getBlockX() + metadata.regionMinX() - metadata.originX() - margin;
        int maxX = pasteOrigin.getBlockX() + metadata.regionMaxX() - metadata.originX() + margin;
        int minZ = pasteOrigin.getBlockZ() + metadata.regionMinZ() - metadata.originZ() - margin;
        int maxZ = pasteOrigin.getBlockZ() + metadata.regionMaxZ() - metadata.originZ() + margin;
        int minChunkX = Math.floorDiv(minX, 16);
        int maxChunkX = Math.floorDiv(maxX, 16);
        int minChunkZ = Math.floorDiv(minZ, 16);
        int maxChunkZ = Math.floorDiv(maxZ, 16);
        List<int[]> chunks = new ArrayList<>(
                (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1)
        );
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new int[]{chunkX, chunkZ});
            }
        }
        return chunks;
    }
}
