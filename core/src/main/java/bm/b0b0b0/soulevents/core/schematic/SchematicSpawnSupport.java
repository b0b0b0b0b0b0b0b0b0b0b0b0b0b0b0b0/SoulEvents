package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicBlendOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicPlacementOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicMarkerOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendMaterialsSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPasteSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;

import java.util.ArrayList;

public final class SchematicSpawnSupport {

    private SchematicSpawnSupport() {
    }

    public static SchematicPlacementSettings resolvePlacement(
            SchematicSettings defaults,
            SchematicSpawnOverrides overrides
    ) {
        if (overrides == null || overrides.placement() == null) {
            return defaults.placement;
        }
        return toPlacement(overrides.placement());
    }

    public static SchematicBlendSettings resolveBlend(
            SchematicSettings defaults,
            SchematicSpawnOverrides overrides
    ) {
        if (overrides == null || overrides.blend() == null) {
            return defaults.blend;
        }
        SchematicBlendOverrides blend = overrides.blend();
        SchematicBlendSettings resolved = new SchematicBlendSettings();
        resolved.enabled = blend.enabled();
        resolved.radius = blend.radius();
        if (blend.blendPreset() != null) {
            resolved.materials.preset = blend.blendPreset();
        }
        resolved.materials.extraReplaceable = new ArrayList<>(blend.extraReplaceable());
        resolved.materials.excludeReplaceable = new ArrayList<>(blend.excludeReplaceable());
        return resolved;
    }

    public static boolean resolveIgnoreAir(
            SchematicSettings defaults,
            SchematicSpawnOverrides overrides,
            Boolean pasteOptionsOverride
    ) {
        if (pasteOptionsOverride != null) {
            return pasteOptionsOverride;
        }
        if (overrides != null && overrides.paste() != null) {
            return overrides.paste().ignoreAir();
        }
        return defaults.paste.ignoreAir;
    }

    public static int resolveBlocksPerTick(SchematicSettings defaults, SchematicSpawnOverrides overrides) {
        if (overrides != null && overrides.paste() != null && overrides.paste().blocksPerTick() > 0) {
            return overrides.paste().blocksPerTick();
        }
        return defaults.paste.blocksPerTick;
    }

    public static int resolveMarkerSpawnCount(SchematicSpawnOverrides overrides) {
        if (overrides != null && overrides.marker() != null) {
            return overrides.marker().effectiveSpawnCount();
        }
        return SchematicMarkerOverrides.defaults().effectiveSpawnCount();
    }

    private static SchematicPlacementSettings toPlacement(SchematicPlacementOverrides source) {
        SchematicPlacementSettings placement = new SchematicPlacementSettings();
        placement.verticalOffset = source.verticalOffset();
        placement.maxSurfaceDelta = source.maxSurfaceDelta();
        placement.terrainAdaptBlocks = source.terrainAdaptBlocks();
        placement.minAirAbove = source.minAirAbove();
        placement.safetyMargin = source.safetyMargin();
        placement.placementProbeStep = source.placementProbeStep();
        placement.rejectLiquids = source.rejectLiquids();
        placement.requireSolidBelow = source.requireSolidBelow();
        applyTerrainMaterials(placement.terrainMaterials, source);
        return placement;
    }

    private static void applyTerrainMaterials(
            SchematicTerrainMaterialsSettings target,
            SchematicPlacementOverrides source
    ) {
        if (source.terrainPreset() != null) {
            target.preset = source.terrainPreset();
        }
        target.extraNaturalTop = new ArrayList<>(source.extraNaturalTop());
        target.extraRemovable = new ArrayList<>(source.extraRemovable());
        target.excludeNaturalTop = new ArrayList<>(source.excludeNaturalTop());
        target.excludeRemovable = new ArrayList<>(source.excludeRemovable());
    }
}
