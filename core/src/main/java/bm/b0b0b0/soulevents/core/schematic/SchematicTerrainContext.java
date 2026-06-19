package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendMaterialsSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;

public final class SchematicTerrainContext {

    private final SchematicTerrainAdapter terrainAdapter;
    private final SchematicLandscapeBlender landscapeBlender;

    private SchematicTerrainContext(
            SchematicTerrainAdapter terrainAdapter,
            SchematicLandscapeBlender landscapeBlender
    ) {
        this.terrainAdapter = terrainAdapter;
        this.landscapeBlender = landscapeBlender;
    }

    public static SchematicTerrainContext from(
            SchematicPlacementSettings placement,
            SchematicBlendSettings blend
    ) {
        SchematicTerrainMaterialsSettings terrainMaterials = placement.terrainMaterials;
        SchematicTerrainAdapter adapter = SchematicTerrainAdapter.from(terrainMaterials);
        SchematicMaterialSet replaceable = new SchematicMaterialSet(
                blend.materials.replaceable,
                SchematicBlendMaterialsSettings.defaultReplaceable()
        );
        return new SchematicTerrainContext(adapter, new SchematicLandscapeBlender(adapter, replaceable));
    }

    public SchematicTerrainAdapter terrainAdapter() {
        return terrainAdapter;
    }

    public SchematicLandscapeBlender landscapeBlender() {
        return landscapeBlender;
    }
}
