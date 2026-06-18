package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Location;

import java.util.UUID;

public record SchematicPasteResult(
        UUID sessionId,
        Location origin,
        int blockCount
) {
}
