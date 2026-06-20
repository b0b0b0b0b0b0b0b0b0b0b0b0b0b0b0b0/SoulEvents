package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.volcano.config.settings.SchematicTypeSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicBlendSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicMarkerSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicPasteSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicPlacementSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicBlendOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicMarkerOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPlacementOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;

public final class SchematicSpawnOverridesFactory {

    private SchematicSpawnOverridesFactory() {
    }

    public static SchematicSpawnOverrides from(SchematicTypeSettings schematic) {
        return new SchematicSpawnOverrides(
                placement(schematic.placement),
                blend(schematic.blend),
                paste(schematic.paste),
                marker(schematic.marker)
        );
    }

    private static SchematicMarkerOverrides marker(TypeSchematicMarkerSettings marker) {
        return new SchematicMarkerOverrides(marker.spawnCount, marker.replaceWithAir);
    }

    private static SchematicPlacementOverrides placement(TypeSchematicPlacementSettings placement) {
        return new SchematicPlacementOverrides(
                placement.verticalOffset,
                placement.maxSurfaceDelta,
                placement.maxSafetyMarginDelta,
                placement.terrainAdaptBlocks,
                placement.terrainApproachRing,
                placement.terrainApproachFrontDepth,
                placement.approachFrontFacing,
                placement.terrainApproachTrimOnly,
                placement.terrainApproachRaggedDensity,
                placement.minAirAbove,
                placement.safetyMargin,
                placement.placementProbeStep,
                placement.rejectLiquids,
                placement.rejectWaterWithinHorizontalBlocks,
                placement.rejectWaterDepthBlocks,
                placement.minWaterClearanceFromEdge,
                placement.minCliffClearanceFromEdge,
                placement.maxCliffDropFromEdge,
                placement.requireSolidBelow,
                placement.terrainMaterials.preset,
                placement.terrainMaterials.extraNaturalTop,
                placement.terrainMaterials.extraRemovable,
                placement.terrainMaterials.excludeNaturalTop,
                placement.terrainMaterials.excludeRemovable
        );
    }

    private static SchematicBlendOverrides blend(TypeSchematicBlendSettings blend) {
        return new SchematicBlendOverrides(
                blend.enabled,
                blend.radius,
                blend.materials.preset,
                blend.materials.extraReplaceable,
                blend.materials.excludeReplaceable
        );
    }

    private static SchematicPasteOverrides paste(TypeSchematicPasteSettings paste) {
        return new SchematicPasteOverrides(paste.ignoreAir, paste.blocksPerTick);
    }
}

