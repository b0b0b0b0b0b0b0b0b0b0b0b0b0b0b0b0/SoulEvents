package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.World;

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
        return from(placement, blend, null, 0, 0, null);
    }

    public static SchematicTerrainContext from(
            SchematicPlacementSettings placement,
            SchematicBlendSettings blend,
            World world,
            int pasteX,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        SchematicTerrainAdapter adapter = world != null && metadata != null
                ? SchematicTerrainAdapter.from(
                placement.terrainMaterials,
                world,
                pasteX,
                pasteZ,
                metadata.floorColumns()
        )
                : SchematicTerrainAdapter.from(placement.terrainMaterials);
        SchematicMaterialSet replaceable = SchematicMaterialSet.blendReplaceable(blend.materials);
        return new SchematicTerrainContext(adapter, new SchematicLandscapeBlender(adapter, replaceable));
    }

    public SchematicTerrainAdapter terrainAdapter() {
        return terrainAdapter;
    }

    public SchematicLandscapeBlender landscapeBlender() {
        return landscapeBlender;
    }
}
