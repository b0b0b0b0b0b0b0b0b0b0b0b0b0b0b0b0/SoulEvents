package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.settings.SchematicTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeSchematicBlendSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeSchematicPasteSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeSchematicPlacementSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicBlendOverrides;
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
                paste(schematic.paste)
        );
    }

    private static SchematicPlacementOverrides placement(TypeSchematicPlacementSettings placement) {
        return new SchematicPlacementOverrides(
                placement.verticalOffset,
                placement.maxSurfaceDelta,
                placement.terrainAdaptBlocks,
                placement.minAirAbove,
                placement.safetyMargin,
                placement.placementProbeStep,
                placement.rejectLiquids,
                placement.requireSolidBelow,
                placement.terrainMaterials.naturalTop,
                placement.terrainMaterials.removable
        );
    }

    private static SchematicBlendOverrides blend(TypeSchematicBlendSettings blend) {
        return new SchematicBlendOverrides(
                blend.enabled,
                blend.radius,
                blend.materials.replaceable
        );
    }

    private static SchematicPasteOverrides paste(TypeSchematicPasteSettings paste) {
        return new SchematicPasteOverrides(paste.ignoreAir, paste.blocksPerTick);
    }
}
