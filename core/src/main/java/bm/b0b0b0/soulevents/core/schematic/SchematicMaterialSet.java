package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicBlendMaterialsSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;
import org.bukkit.Material;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public final class SchematicMaterialSet {

    private final Set<Material> materials;

    private SchematicMaterialSet(Set<Material> materials) {
        this.materials = materials;
    }

    public static SchematicMaterialSet terrainNaturalTop(SchematicTerrainMaterialsSettings settings) {
        return compose(
                SchematicMaterialCatalog.naturalTop(settings.preset),
                settings.extraNaturalTop,
                settings.excludeNaturalTop
        );
    }

    public static SchematicMaterialSet terrainRemovable(SchematicTerrainMaterialsSettings settings) {
        return compose(
                SchematicMaterialCatalog.removable(settings.preset),
                settings.extraRemovable,
                settings.excludeRemovable
        );
    }

    public static SchematicMaterialSet blendReplaceable(SchematicBlendMaterialsSettings settings) {
        return compose(
                SchematicMaterialCatalog.blendReplaceable(settings.preset),
                settings.extraReplaceable,
                settings.excludeReplaceable
        );
    }

    public boolean contains(Material material) {
        return materials.contains(material);
    }

    private static SchematicMaterialSet compose(
            Set<Material> base,
            Collection<Material> extra,
            Collection<Material> exclude
    ) {
        EnumSet<Material> merged = EnumSet.noneOf(Material.class);
        merged.addAll(base);
        if (extra != null) {
            merged.addAll(extra);
        }
        if (exclude != null) {
            exclude.forEach(merged::remove);
        }
        return new SchematicMaterialSet(Set.copyOf(merged));
    }
}
