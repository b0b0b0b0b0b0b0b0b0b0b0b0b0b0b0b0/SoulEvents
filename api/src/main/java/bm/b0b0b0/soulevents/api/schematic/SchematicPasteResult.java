package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Location;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record SchematicPasteResult(
        UUID sessionId,
        boolean success,
        Location pasteOrigin,
        Location chestAnchor,
        List<Location> chestAnchors,
        int blockCount,
        Optional<String> errorKey
) {

    public SchematicPasteResult {
        if (chestAnchors == null || chestAnchors.isEmpty()) {
            chestAnchors = chestAnchor != null ? List.of(chestAnchor) : List.of();
        } else {
            chestAnchors = List.copyOf(chestAnchors);
        }
        if (chestAnchor == null && !chestAnchors.isEmpty()) {
            chestAnchor = chestAnchors.getFirst();
        }
    }

    public static SchematicPasteResult failed(UUID sessionId, String errorKey) {
        return new SchematicPasteResult(sessionId, false, null, null, List.of(), 0, Optional.of(errorKey));
    }
}
