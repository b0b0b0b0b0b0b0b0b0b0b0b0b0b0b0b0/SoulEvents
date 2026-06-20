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
            SchematicTerrainAdapter terrainAdapter,
            int horizontalMargin,
            int terrainAdapt
    ) {
        CompletableFuture<PreparedRegion> future = new CompletableFuture<>();
        List<WorldEditSchematicBridge.BlockSnapshot> snapshots = new ArrayList<>();
        List<int[]> captureSteps = SchematicRegionBounds.buildUndoCaptureSteps(
                metadata,
                horizontalMargin,
                terrainAdapt,
                placement
        );
        int pasteX = pasteOrigin.getBlockX();
        int pasteY = pasteOrigin.getBlockY();
        int pasteZ = pasteOrigin.getBlockZ();

        captureBatch(plugin, world, pasteX, pasteY, pasteZ, captureSteps, snapshots, 0, () ->
                vegetationClearBatch(plugin, world, metadata, placement, pasteX, pasteY, pasteZ, 0, () -> {
                    Runnable finish = () -> future.complete(new PreparedRegion(
                            List.copyOf(snapshots),
                            pasteX,
                            pasteY,
                            pasteZ
                    ));
                    if (!SchematicTerrainAdapter.needsPerimeterAdapt(placement)
                            && !SchematicTerrainAdapter.needsApproachAdapt(placement)) {
                        finish.run();
                        return;
                    }
                    adaptBatch(
                            plugin,
                            world,
                            metadata,
                            placement,
                            terrainAdapter,
                            pasteX,
                            pasteY,
                            pasteZ,
                            0,
                            finish
                    );
                }, future), future);
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
        List<int[]> columns = vegetationClearColumns(metadata, placement);
        if (columns.isEmpty()) {
            onComplete.run();
            return;
        }
        int end = Math.min(columns.size(), columnIndex + CLEAR_COLUMNS_PER_TICK);
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int clearTop = pasteY + maxDy + Math.max(0, placement.minAirAbove);
        for (int index = columnIndex; index < end; index++) {
            int[] column = columns.get(index);
            int worldX = pasteX + column[0];
            int worldZ = pasteZ + column[1];
            NaturalSurfaceResolver.clearFootingColumn(world, worldX, worldZ, clearTop);
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

    private static List<int[]> vegetationClearColumns(
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
        List<int[]> result = new ArrayList<>();

        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();
        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                addClearColumn(result, seen, dx, dz);
            }
        }

        int approachRing = Math.max(0, placement.terrainApproachRing);
        int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
        for (SchematicApproachColumn column : SchematicFloorSupport.approachAdaptColumns(
                metadata.floorColumns(),
                metadata,
                approachRing,
                frontDepth,
                placement.approachFrontFacing
        )) {
            addClearColumn(result, seen, column.dx(), column.dz());
        }
        return result;
    }

    private static void addClearColumn(List<int[]> result, java.util.Set<Long> seen, int dx, int dz) {
        long key = SchematicFloorSupport.columnKey(dx, dz);
        if (seen.add(key)) {
            result.add(new int[] {dx, dz});
        }
    }

    private static void adaptBatch(
            Plugin plugin,
            World world,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            SchematicTerrainAdapter terrainAdapter,
            int pasteX,
            int pasteY,
            int pasteZ,
            int columnIndex,
            Runnable onComplete
    ) {
        List<SchematicFloorColumn> floorColumns = SchematicFloorSupport.footprintAdaptColumns(metadata.floorColumns());
        int approachRing = Math.max(0, placement.terrainApproachRing);
        int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
        List<SchematicApproachColumn> approachColumns = SchematicFloorSupport.approachAdaptColumns(
                metadata.floorColumns(),
                metadata,
                approachRing,
                frontDepth,
                placement.approachFrontFacing
        );

        boolean adaptPerimeter = SchematicTerrainAdapter.needsPerimeterAdapt(placement) && !floorColumns.isEmpty();
        boolean adaptApproach = SchematicTerrainAdapter.needsApproachAdapt(placement) && !approachColumns.isEmpty();
        if (!adaptPerimeter && !adaptApproach) {
            onComplete.run();
            return;
        }

        int perimeterCount = adaptPerimeter ? floorColumns.size() : 0;
        int totalColumns = perimeterCount + (adaptApproach ? approachColumns.size() : 0);
        int end = Math.min(totalColumns, columnIndex + ADAPT_COLUMNS_PER_TICK);
        int limit = Math.max(1, placement.terrainAdaptBlocks);

        for (int index = columnIndex; index < end; index++) {
            if (index < perimeterCount) {
                SchematicFloorColumn column = floorColumns.get(index);
                terrainAdapter.adaptColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY + column.floorDy(),
                        limit
                );
            } else {
                SchematicApproachColumn column = approachColumns.get(index - perimeterCount);
                terrainAdapter.adaptApproachColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY,
                        column,
                        SchematicFloorSupport.approachAdaptRingDepth(column, approachRing, frontDepth),
                        limit,
                        placement,
                        pasteX,
                        pasteZ
                );
            }
        }
        if (end >= totalColumns) {
            onComplete.run();
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> adaptBatch(
                        plugin, world, metadata, placement, terrainAdapter, pasteX, pasteY, pasteZ, end, onComplete
                ),
                1L
        );
    }
}
