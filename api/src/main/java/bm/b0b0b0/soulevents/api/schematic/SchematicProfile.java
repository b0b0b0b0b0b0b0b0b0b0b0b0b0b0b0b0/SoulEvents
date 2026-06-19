package bm.b0b0b0.soulevents.api.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;

import java.util.List;

public record SchematicProfile(
        String id,
        int sizeX,
        int sizeY,
        int sizeZ,
        int chestOffsetX,
        int chestOffsetY,
        int chestOffsetZ,
        int verticalOffset,
        int maxSurfaceDelta,
        int minAirAbove,
        int safetyMargin,
        boolean rejectLiquids,
        boolean requireSolidBelow,
        List<FlatSurfaceOffset> footprint,
        List<FlatSurfaceOffset> surfaceProbe
) {
}
