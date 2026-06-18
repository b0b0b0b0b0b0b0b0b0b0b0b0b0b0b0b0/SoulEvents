package bm.b0b0b0.soulevents.api.schematic;

import java.util.UUID;

public record SchematicPasteOptions(
        UUID sessionId,
        boolean landscapeBlend,
        int blendRadius,
        boolean ignoreAir
) {
}
