package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Material;

import java.util.List;

public record SchematicBlendOverrides(
        boolean enabled,
        int radius,
        SchematicBlendPreset blendPreset,
        List<Material> extraReplaceable,
        List<Material> excludeReplaceable
) {
    public SchematicBlendOverrides {
        extraReplaceable = extraReplaceable == null ? List.of() : List.copyOf(extraReplaceable);
        excludeReplaceable = excludeReplaceable == null ? List.of() : List.copyOf(excludeReplaceable);
    }
}
