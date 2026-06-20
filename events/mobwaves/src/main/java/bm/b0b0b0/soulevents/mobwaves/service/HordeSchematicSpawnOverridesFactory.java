package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeSchematicSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeSchematicBlendSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeSchematicMarkerSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeSchematicPasteSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeSchematicPlacementSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicBlendOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicMarkerOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPlacementOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;

public final class HordeSchematicSpawnOverridesFactory {

    private HordeSchematicSpawnOverridesFactory() {
    }

    public static SchematicSpawnOverrides from(HordeSchematicSettings schematic) {
        return new SchematicSpawnOverrides(
                placement(schematic.placement),
                blend(schematic.blend),
                paste(schematic.paste),
                marker(schematic.marker)
        );
    }

    private static SchematicMarkerOverrides marker(HordeSchematicMarkerSettings marker) {
        return new SchematicMarkerOverrides(marker.spawnCount, marker.replaceWithAir);
    }

    private static SchematicPlacementOverrides placement(HordeSchematicPlacementSettings placement) {
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
                placement.terrainPerimeterRaggedTrim,
                placement.terrainPerimeterRaggedOutwardDepth,
                placement.minAirAbove,
                placement.safetyMargin,
                placement.placementProbeStep,
                placement.rejectLiquids,
                placement.rejectWaterWithinHorizontalBlocks,
                placement.rejectWaterDepthBlocks,
                placement.minWaterClearanceFromEdge,
                placement.minCliffClearanceFromEdge,
                placement.maxCliffDropFromEdge,
                placement.minOutwardMountainRiseSteps,
                placement.mountainSlopeScanDepth,
                placement.requireSolidBelow,
                placement.terrainMaterials.preset,
                placement.terrainMaterials.extraNaturalTop,
                placement.terrainMaterials.extraRemovable,
                placement.terrainMaterials.excludeNaturalTop,
                placement.terrainMaterials.excludeRemovable
        );
    }

    private static SchematicBlendOverrides blend(HordeSchematicBlendSettings blend) {
        return new SchematicBlendOverrides(
                blend.enabled,
                blend.radius,
                blend.materials.preset,
                blend.materials.extraReplaceable,
                blend.materials.excludeReplaceable
        );
    }

    private static SchematicPasteOverrides paste(HordeSchematicPasteSettings paste) {
        return new SchematicPasteOverrides(paste.ignoreAir, paste.blocksPerTick);
    }
}
