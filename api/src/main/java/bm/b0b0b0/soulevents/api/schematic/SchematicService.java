package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Location;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SchematicService {

    CompletableFuture<SchematicPasteResult> paste(String schematicId, Location anchor, SchematicPasteOptions options);

    CompletableFuture<Void> undo(UUID sessionId);

    void reload();
}
