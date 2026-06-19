package bm.b0b0b0.soulevents.api.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SchematicService {

    Collection<String> schematicIds();

    Optional<SchematicProfile> profile(String schematicId);

    Optional<SchematicWorldBounds> worldBounds(String schematicId, Location pasteOrigin);

    List<FlatSurfaceOffset> footprint(String schematicId);

    Optional<Location> resolvePasteOrigin(World world, int blockX, int blockZ, String schematicId);

    Optional<Location> resolveChestAnchor(Location pasteOrigin, String schematicId);

    CompletableFuture<SchematicPasteResult> paste(String schematicId, Location pasteOrigin, SchematicPasteOptions options);

    CompletableFuture<Void> undo(UUID sessionId);

    void reload();
}
