package bm.b0b0b0.soulevents.api.schematic;

import java.util.List;

public record SchematicBlendOverrides(
        boolean enabled,
        int radius,
        List<String> replaceableMaterials
) {
    public SchematicBlendOverrides {
        replaceableMaterials = replaceableMaterials == null ? List.of() : List.copyOf(replaceableMaterials);
    }
}
