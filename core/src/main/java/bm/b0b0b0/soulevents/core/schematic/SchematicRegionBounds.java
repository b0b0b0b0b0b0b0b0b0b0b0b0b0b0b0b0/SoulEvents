package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SchematicRegionBounds {

    private SchematicRegionBounds() {
    }

    public record VolumeBounds(
            int minDx,
            int maxDx,
            int minDy,
            int maxDy,
            int minDz,
            int maxDz
    ) {
        public long blockCount() {
            long sizeX = (long) maxDx - minDx + 1L;
            long sizeY = (long) maxDy - minDy + 1L;
            long sizeZ = (long) maxDz - minDz + 1L;
            return sizeX * sizeY * sizeZ;
        }
    }

    public static VolumeBounds captureBounds(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int verticalBelow,
            int verticalAbove
    ) {
        int margin = Math.max(0, horizontalMargin);
        int below = Math.max(0, verticalBelow);
        int above = Math.max(0, verticalAbove);
        return new VolumeBounds(
                metadata.regionMinX() - metadata.originX() - margin,
                metadata.regionMaxX() - metadata.originX() + margin,
                metadata.regionMinY() - metadata.originY() - below,
                metadata.regionMaxY() - metadata.originY() + above,
                metadata.regionMinZ() - metadata.originZ() - margin,
                metadata.regionMaxZ() - metadata.originZ() + margin
        );
    }

    public static int estimateCaptureBlockCount(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int verticalBelow,
            int verticalAbove
    ) {
        long blocks = captureBounds(metadata, horizontalMargin, verticalBelow, verticalAbove).blockCount();
        return blocks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) blocks;
    }

    public static int estimateUndoCaptureBlockCount(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int terrainAdaptBelow,
            int terrainApproachRing,
            int blendRadius
    ) {
        int total = buildUndoCaptureSteps(metadata, horizontalMargin, terrainAdaptBelow, terrainApproachRing).size();
        total += SchematicLandscapeBlender.estimatePreBlendCaptureCount(metadata, blendRadius);
        return total;
    }

    public static int estimateUndoCaptureBlockCount(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int terrainAdaptBelow,
            int blendRadius
    ) {
        return estimateUndoCaptureBlockCount(metadata, horizontalMargin, terrainAdaptBelow, 0, blendRadius);
    }

    public static int estimateUndoCaptureBlockCount(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int terrainAdaptBelow
    ) {
        return estimateUndoCaptureBlockCount(metadata, horizontalMargin, terrainAdaptBelow, 0);
    }

    public static List<FlatSurfaceOffset> horizontalColumns(SchematicDefinition.SchematicMetadata metadata) {
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();
        List<FlatSurfaceOffset> columns = new ArrayList<>((maxDx - minDx + 1) * (maxDz - minDz + 1));
        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                columns.add(new FlatSurfaceOffset(dx, dz));
            }
        }
        return columns;
    }

    public static List<int[]> buildUndoCaptureSteps(
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int terrainAdaptBelow,
            int terrainApproachRing
    ) {
        VolumeBounds main = captureBounds(metadata, horizontalMargin, 0, 0);
        Set<Long> seen = new HashSet<>();
        List<int[]> steps = new ArrayList<>((int) Math.min(main.blockCount(), Integer.MAX_VALUE));
        appendVolumeSteps(steps, seen, main);
        if (terrainAdaptBelow > 0) {
            int approachRing = Math.max(0, terrainApproachRing);
            for (SchematicFloorColumn column : SchematicFloorSupport.perimeterFloorColumns(metadata.floorColumns())) {
                appendAdaptColumnSteps(steps, seen, column.dx(), column.dz(), column.floorDy(), terrainAdaptBelow);
            }
            for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(
                    metadata.floorColumns(),
                    approachRing
            )) {
                appendAdaptColumnSteps(
                        steps,
                        seen,
                        column.dx(),
                        column.dz(),
                        column.edgeReferenceDy(),
                        terrainAdaptBelow + approachRing
                );
            }
        }
        return steps;
    }

    private static void appendAdaptColumnSteps(
            List<int[]> steps,
            Set<Long> seen,
            int dx,
            int dz,
            int centerDy,
            int verticalRange
    ) {
        for (int dy = centerDy - verticalRange; dy <= centerDy + verticalRange; dy++) {
            appendStep(steps, seen, dx, dy, dz);
        }
    }

    private static void appendVolumeSteps(List<int[]> steps, Set<Long> seen, VolumeBounds bounds) {
        for (int dx = bounds.minDx(); dx <= bounds.maxDx(); dx++) {
            for (int dz = bounds.minDz(); dz <= bounds.maxDz(); dz++) {
                for (int dy = bounds.minDy(); dy <= bounds.maxDy(); dy++) {
                    appendStep(steps, seen, dx, dy, dz);
                }
            }
        }
    }

    private static void appendStep(List<int[]> steps, Set<Long> seen, int dx, int dy, int dz) {
        long key = WorldEditSchematicBridge.snapshotKey(dx, dy, dz);
        if (seen.add(key)) {
            steps.add(new int[]{dx, dy, dz});
        }
    }
}
