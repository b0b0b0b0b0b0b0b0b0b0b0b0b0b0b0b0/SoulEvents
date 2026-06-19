package bm.b0b0b0.soulevents.api.schematic;

import java.util.List;

public record SchematicPlacementOverrides(
        int verticalOffset,
        int maxSurfaceDelta,
        int terrainAdaptBlocks,
        int minAirAbove,
        int safetyMargin,
        int placementProbeStep,
        boolean rejectLiquids,
        boolean requireSolidBelow,
        List<String> naturalTopMaterials,
        List<String> removableMaterials
) {
    public SchematicPlacementOverrides {
        naturalTopMaterials = naturalTopMaterials == null ? List.of() : List.copyOf(naturalTopMaterials);
        removableMaterials = removableMaterials == null ? List.of() : List.copyOf(removableMaterials);
    }
}
