package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteResult;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SchematicServiceImpl implements SchematicService {

    private final Plugin plugin;

    public SchematicServiceImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<SchematicPasteResult> paste(String schematicId, Location anchor, SchematicPasteOptions options) {
        return CompletableFuture.supplyAsync(() -> new SchematicPasteResult(options.sessionId(), anchor.clone(), 0));
    }

    @Override
    public CompletableFuture<Void> undo(UUID sessionId) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void reload() {
    }
}
