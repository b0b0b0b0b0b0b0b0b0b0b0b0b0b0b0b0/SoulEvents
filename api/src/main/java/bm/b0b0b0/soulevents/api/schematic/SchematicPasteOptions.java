package bm.b0b0b0.soulevents.api.schematic;

import java.util.UUID;

public record SchematicPasteOptions(
        UUID sessionId,
        Boolean landscapeBlendOverride,
        Integer blendRadiusOverride,
        Boolean ignoreAirOverride
) {

    public static SchematicPasteOptions of(UUID sessionId) {
        return new SchematicPasteOptions(sessionId, null, null, null);
    }

    public static SchematicPasteOptions legacy(UUID sessionId, boolean landscapeBlend, int blendRadius, boolean ignoreAir) {
        return new SchematicPasteOptions(sessionId, landscapeBlend, blendRadius, ignoreAir);
    }
}
