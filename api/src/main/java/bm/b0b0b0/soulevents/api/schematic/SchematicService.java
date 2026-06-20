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

    List<FlatSurfaceOffset> cornerFootprintSamples(String schematicId);

    List<FlatSurfaceOffset> perimeterFootprintSamples(String schematicId);

    CompletableFuture<Void> prepareSpawnFootprint(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            String schematicId,
            int blockMargin
    );

    CompletableFuture<Void> prepareSpawnSearchFootprint(
            World world,
            String schematicId,
            int blockMargin,
            List<int[]> blockOrigins
    );

    CompletableFuture<Void> prepareSpawnSearchArea(
            World world,
            int centerBlockX,
            int centerBlockZ,
            int blockRadius
    );

    default CompletableFuture<Void> prepareSpawnFootprint(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            String schematicId
    ) {
        return prepareSpawnFootprint(world, pasteOriginBlockX, pasteOriginBlockZ, schematicId, 0);
    }

    default Optional<Location> resolvePasteOrigin(World world, int blockX, int blockZ, String schematicId) {
        return resolvePasteOrigin(world, blockX, blockZ, schematicId, null);
    }

    Optional<Location> resolvePasteOrigin(
            World world,
            int blockX,
            int blockZ,
            String schematicId,
            SchematicSpawnOverrides overrides
    );

    default SchematicPlacementResolution resolvePasteOriginDetailed(
            World world,
            int blockX,
            int blockZ,
            String schematicId
    ) {
        return resolvePasteOriginDetailed(world, blockX, blockZ, schematicId, null);
    }

    SchematicPlacementResolution resolvePasteOriginDetailed(
            World world,
            int blockX,
            int blockZ,
            String schematicId,
            SchematicSpawnOverrides overrides
    );

    Optional<Location> resolveChestAnchor(Location pasteOrigin, String schematicId);

    default CompletableFuture<SchematicPasteResult> paste(
            String schematicId,
            Location pasteOrigin,
            SchematicPasteOptions options
    ) {
        return paste(schematicId, pasteOrigin, options, null);
    }

    CompletableFuture<SchematicPasteResult> paste(
            String schematicId,
            Location pasteOrigin,
            SchematicPasteOptions options,
            SchematicSpawnOverrides overrides
    );

    CompletableFuture<Void> undo(UUID sessionId);

    void reload();
}
