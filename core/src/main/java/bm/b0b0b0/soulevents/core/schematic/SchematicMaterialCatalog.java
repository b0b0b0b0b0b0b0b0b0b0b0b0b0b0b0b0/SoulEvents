package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicBlendPreset;
import bm.b0b0b0.soulevents.api.schematic.SchematicTerrainPreset;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumSet;
import java.util.Set;

public final class SchematicMaterialCatalog {

    private static final Set<Material> OVERWORLD_NATURAL_TOP = Set.of(
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.SAND,
            Material.RED_SAND,
            Material.SNOW_BLOCK,
            Material.MOSS_BLOCK
    );

    private static final Set<Material> OVERWORLD_REMOVABLE;

    private static final Set<Material> DESERT_NATURAL_TOP = Set.of(
            Material.SAND,
            Material.RED_SAND,
            Material.SANDSTONE,
            Material.RED_SANDSTONE,
            Material.TERRACOTTA
    );

    private static final Set<Material> DESERT_REMOVABLE;

    private static final Set<Material> OVERWORLD_BLEND_REPLACEABLE = Set.of(
            Material.STONE,
            Material.COBBLESTONE,
            Material.DEEPSLATE,
            Material.COBBLED_DEEPSLATE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.COARSE_DIRT,
            Material.SAND,
            Material.GRAVEL,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.MOSS_BLOCK
    );

    static {
        EnumSet<Material> removable = EnumSet.copyOf(OVERWORLD_NATURAL_TOP);
        removable.addAll(Set.of(
                Material.GRAVEL,
                Material.SNOW,
                Material.STONE,
                Material.COBBLESTONE,
                Material.ANDESITE,
                Material.DIORITE,
                Material.GRANITE
        ));
        removable.addAll(Tag.FLOWERS.getValues());
        removable.addAll(Set.of(
                Material.TALL_GRASS,
                Material.SHORT_GRASS,
                Material.FERN,
                Material.LARGE_FERN,
                Material.DEAD_BUSH
        ));
        OVERWORLD_REMOVABLE = Set.copyOf(removable);

        EnumSet<Material> desertRemovable = EnumSet.copyOf(DESERT_NATURAL_TOP);
        desertRemovable.add(Material.GRAVEL);
        desertRemovable.addAll(Tag.FLOWERS.getValues());
        desertRemovable.addAll(Set.of(
                Material.DEAD_BUSH,
                Material.CACTUS,
                Material.SHORT_GRASS,
                Material.TALL_GRASS
        ));
        DESERT_REMOVABLE = Set.copyOf(desertRemovable);
    }

    private SchematicMaterialCatalog() {
    }

    public static Set<Material> naturalTop(SchematicTerrainPreset preset) {
        return switch (preset) {
            case OVERWORLD -> OVERWORLD_NATURAL_TOP;
            case DESERT -> DESERT_NATURAL_TOP;
            case CUSTOM -> Set.of();
        };
    }

    public static Set<Material> removable(SchematicTerrainPreset preset) {
        return switch (preset) {
            case OVERWORLD -> OVERWORLD_REMOVABLE;
            case DESERT -> DESERT_REMOVABLE;
            case CUSTOM -> Set.of();
        };
    }

    public static Set<Material> blendReplaceable(SchematicBlendPreset preset) {
        return switch (preset) {
            case OVERWORLD -> OVERWORLD_BLEND_REPLACEABLE;
            case CUSTOM -> Set.of();
        };
    }
}
