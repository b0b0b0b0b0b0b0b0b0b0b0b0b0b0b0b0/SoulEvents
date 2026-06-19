package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public record SchematicPasteResult(
        UUID sessionId,
        boolean success,
        Location pasteOrigin,
        Location chestAnchor,
        int blockCount,
        Optional<String> errorKey
) {

    public static SchematicPasteResult failed(UUID sessionId, String errorKey) {
        return new SchematicPasteResult(sessionId, false, null, null, 0, Optional.of(errorKey));
    }
}
