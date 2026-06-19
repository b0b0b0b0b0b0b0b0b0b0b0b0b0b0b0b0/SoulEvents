package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class SchematicMaterialSet {

    private final Set<Material> materials;

    public SchematicMaterialSet(List<String> configured, List<String> fallback) {
        this.materials = parse(configured, fallback);
    }

    public boolean contains(Material material) {
        return materials.contains(material);
    }

    private static Set<Material> parse(List<String> configured, List<String> fallback) {
        Set<Material> parsed = parseNames(configured);
        if (parsed.isEmpty()) {
            parsed = parseNames(fallback);
        }
        return Set.copyOf(parsed);
    }

    private static Set<Material> parseNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        EnumSet<Material> materials = EnumSet.noneOf(Material.class);
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Material material = Material.matchMaterial(name.trim().toUpperCase());
            if (material != null && material.isBlock()) {
                materials.add(material);
            }
        }
        return materials;
    }
}
