package bm.b0b0b0.soulevents.api.schematic;

public record SchematicPlacementOverrides(
        int verticalOffset,
        int maxSurfaceDelta,
        int terrainAdaptBlocks,
        int minAirAbove,
        int safetyMargin,
        int placementProbeStep,
        boolean rejectLiquids,
        boolean requireSolidBelow
) {
}
