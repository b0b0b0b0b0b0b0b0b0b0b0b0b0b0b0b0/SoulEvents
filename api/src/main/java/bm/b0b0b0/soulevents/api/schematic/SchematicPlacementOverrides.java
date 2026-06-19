package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Material;

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
        SchematicTerrainPreset terrainPreset,
        List<Material> extraNaturalTop,
        List<Material> extraRemovable,
        List<Material> excludeNaturalTop,
        List<Material> excludeRemovable
) {
    public SchematicPlacementOverrides {
        extraNaturalTop = extraNaturalTop == null ? List.of() : List.copyOf(extraNaturalTop);
        extraRemovable = extraRemovable == null ? List.of() : List.copyOf(extraRemovable);
        excludeNaturalTop = excludeNaturalTop == null ? List.of() : List.copyOf(excludeNaturalTop);
        excludeRemovable = excludeRemovable == null ? List.of() : List.copyOf(excludeRemovable);
    }
}
