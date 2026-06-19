package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class SchematicRegionPreparer {

    static final int CAPTURE_BLOCKS_PER_TICK = 8_192;
    static final int ADAPT_COLUMNS_PER_TICK = 64;
    static final int CLEAR_COLUMNS_PER_TICK = 64;

    private SchematicRegionPreparer() {
    }

    public record PreparedRegion(
            List<WorldEditSchematicBridge.BlockSnapshot> snapshots,
            int pasteX,
            int pasteY,
            int pasteZ
    ) {
    }

    public static CompletableFuture<PreparedRegion> prepareAsync(
            Plugin plugin,
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            int horizontalMargin,
            int terrainAdapt
    ) {
        CompletableFuture<PreparedRegion> future = new CompletableFuture<>();
        List<WorldEditSchematicBridge.BlockSnapshot> snapshots = new ArrayList<>();
        List<int[]> captureSteps = SchematicRegionBounds.buildUndoCaptureSteps(
                metadata,
                horizontalMargin,
                terrainAdapt
        );
        int pasteX = pasteOrigin.getBlockX();
        int pasteY = pasteOrigin.getBlockY();
        int pasteZ = pasteOrigin.getBlockZ();

        captureBatch(plugin, world, pasteX, pasteY, pasteZ, captureSteps, snapshots, 0, () ->
                vegetationClearBatch(plugin, world, metadata, placement, pasteX, pasteY, pasteZ, 0, () ->
                        adaptBatch(plugin, world, metadata, placement, pasteX, pasteY, pasteZ, 0, () ->
                                future.complete(new PreparedRegion(List.copyOf(snapshots), pasteX, pasteY, pasteZ))
                        ), future), future);
        return future;
    }

    private static void captureBatch(
            Plugin plugin,
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            List<int[]> steps,
            List<WorldEditSchematicBridge.BlockSnapshot> snapshots,
            int index,
            Runnable onComplete,
            CompletableFuture<?> failureTarget
    ) {
        int end = Math.min(steps.size(), index + CAPTURE_BLOCKS_PER_TICK);
        for (int cursor = index; cursor < end; cursor++) {
            int[] offset = steps.get(cursor);
            snapshots.add(WorldEditSchematicBridge.snapshotBlock(
                    world,
                    pasteX + offset[0],
                    pasteY + offset[1],
                    pasteZ + offset[2]
            ));
        }
        if (end >= steps.size()) {
            onComplete.run();
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> captureBatch(plugin, world, pasteX, pasteY, pasteZ, steps, snapshots, end, onComplete, failureTarget),
                1L
        );
    }

    private static void vegetationClearBatch(
            Plugin plugin,
            World world,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            int pasteX,
            int pasteY,
            int pasteZ,
            int columnIndex,
            Runnable onComplete,
            CompletableFuture<?> failureTarget
    ) {
        List<int[]> columns = vegetationClearColumns(metadata);
        if (columns.isEmpty()) {
            onComplete.run();
            return;
        }
        int end = Math.min(columns.size(), columnIndex + CLEAR_COLUMNS_PER_TICK);
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int clearTop = pasteY + maxDy + Math.max(0, placement.minAirAbove);
        for (int index = columnIndex; index < end; index++) {
            int[] column = columns.get(index);
            int worldX = pasteX + column[0];
            int worldZ = pasteZ + column[1];
            int groundY = NaturalSurfaceResolver.groundY(world, worldX, worldZ);
            int clearBottom = Math.max(groundY + 1, pasteY + minDy);
            NaturalSurfaceResolver.clearColumnObstructions(world, worldX, worldZ, clearBottom, clearTop);
        }
        if (end >= columns.size()) {
            onComplete.run();
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> vegetationClearBatch(
                        plugin, world, metadata, placement, pasteX, pasteY, pasteZ, end, onComplete, failureTarget
                ),
                1L
        );
    }

    private static List<int[]> vegetationClearColumns(SchematicDefinition.SchematicMetadata metadata) {
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();
        List<int[]> columns = new ArrayList<>((maxDx - minDx + 1) * (maxDz - minDz + 1));
        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                columns.add(new int[]{dx, dz});
            }
        }
        return columns;
    }

    private static void adaptBatch(
            Plugin plugin,
            World world,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            int pasteX,
            int pasteY,
            int pasteZ,
            int columnIndex,
            Runnable onComplete
    ) {
        List<FlatSurfaceOffset> footprint = metadata.footprint();
        if (placement.terrainAdaptBlocks <= 0 || footprint.isEmpty()) {
            onComplete.run();
            return;
        }
        int end = Math.min(footprint.size(), columnIndex + ADAPT_COLUMNS_PER_TICK);
        int floorWorldY = pasteY + metadata.regionMinY() - metadata.originY();
        int limit = placement.terrainAdaptBlocks;
        for (int index = columnIndex; index < end; index++) {
            FlatSurfaceOffset offset = footprint.get(index);
            SchematicTerrainAdapter.adaptColumn(
                    world,
                    pasteX + offset.dx(),
                    pasteZ + offset.dz(),
                    floorWorldY,
                    limit
            );
        }
        if (end >= footprint.size()) {
            onComplete.run();
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> adaptBatch(plugin, world, metadata, placement, pasteX, pasteY, pasteZ, end, onComplete),
                1L
        );
    }
}
