package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteResult;
import bm.b0b0b0.soulevents.api.schematic.SchematicProfile;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SchematicServiceImpl implements SchematicService {

    private final Plugin plugin;
    private final SchematicCatalog catalog;
    private final SchematicPlacementValidator placementValidator;
    private final SchematicPasteQueue pasteQueue;
    private final SchematicSessionUndo sessionUndo;
    private final WorldEditSchematicBridge worldEditBridge;

    public SchematicServiceImpl(Plugin plugin) {
        this.plugin = plugin;
        this.catalog = new SchematicCatalog(plugin);
        this.placementValidator = new SchematicPlacementValidator();
        this.pasteQueue = new SchematicPasteQueue();
        this.sessionUndo = new SchematicSessionUndo();
        this.worldEditBridge = new WorldEditSchematicBridge();
        catalog.reload();
    }

    public Optional<SchematicDefinition> definition(String schematicId) {
        return catalog.get(schematicId);
    }

    @Override
    public Collection<String> schematicIds() {
        return catalog.ids();
    }

    @Override
    public Optional<SchematicProfile> profile(String schematicId) {
        return catalog.profile(schematicId);
    }

    @Override
    public Optional<SchematicWorldBounds> worldBounds(String schematicId, Location pasteOrigin) {
        if (pasteOrigin.getWorld() == null) {
            return Optional.empty();
        }
        return catalog.get(schematicId)
                .filter(SchematicDefinition::isReady)
                .map(definition -> toWorldBounds(pasteOrigin, definition.metadata()));
    }

    private static SchematicWorldBounds toWorldBounds(
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        int baseX = pasteOrigin.getBlockX();
        int baseY = pasteOrigin.getBlockY();
        int baseZ = pasteOrigin.getBlockZ();
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();
        return new SchematicWorldBounds(
                baseX + minDx,
                baseY + minDy,
                baseZ + minDz,
                baseX + maxDx,
                baseY + maxDy,
                baseZ + maxDz
        );
    }

    @Override
    public List<FlatSurfaceOffset> footprint(String schematicId) {
        return catalog.get(schematicId)
                .filter(SchematicDefinition::isReady)
                .map(definition -> definition.metadata().surfaceProbe())
                .orElse(List.of(new FlatSurfaceOffset(0, 0)));
    }

    @Override
    public Optional<Location> resolvePasteOrigin(World world, int blockX, int blockZ, String schematicId) {
        return catalog.get(schematicId).flatMap(definition -> placementValidator.resolve(world, blockX, blockZ, definition));
    }

    @Override
    public Optional<Location> resolveChestAnchor(Location pasteOrigin, String schematicId) {
        return catalog.get(schematicId)
                .filter(SchematicDefinition::isReady)
                .map(definition -> {
                    SchematicDefinition.SchematicMetadata metadata = definition.metadata();
                    return new Location(
                            pasteOrigin.getWorld(),
                            pasteOrigin.getBlockX() + metadata.chestOffsetX(),
                            pasteOrigin.getBlockY() + metadata.chestOffsetY(),
                            pasteOrigin.getBlockZ() + metadata.chestOffsetZ()
                    );
                });
    }

    @Override
    public CompletableFuture<SchematicPasteResult> paste(
            String schematicId,
            Location pasteOrigin,
            SchematicPasteOptions options
    ) {
        Optional<SchematicDefinition> definitionOptional = catalog.get(schematicId);
        if (definitionOptional.isEmpty()) {
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.unknown")
            );
        }
        SchematicDefinition definition = definitionOptional.get();
        if (!definition.isReady()) {
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.not-ready")
            );
        }
        if (!SchematicMarkerScanner.isFaweAvailable()) {
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.fawe-missing")
            );
        }
        World world = pasteOrigin.getWorld();
        if (world == null) {
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.invalid-world")
            );
        }

        SchematicDefinition.SchematicMetadata metadata = definition.metadata();
        Location normalizedOrigin = normalizePasteOrigin(pasteOrigin);
        Optional<Location> chestOptional = resolveChestAnchor(normalizedOrigin, schematicId);
        if (chestOptional.isEmpty()) {
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.invalid-chest")
            );
        }
        Location chestAnchor = chestOptional.get();
        boolean ignoreAir = options.ignoreAirOverride() != null
                ? options.ignoreAirOverride()
                : definition.settings().paste.ignoreAir;

        CompletableFuture<SchematicPasteResult> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            int undoBlocks = undoBlockCount(metadata);
            int undoLimit = definition.settings().paste.maxUndoBlocks;
            if (undoLimit > 0 && undoBlocks > undoLimit) {
                future.complete(SchematicPasteResult.failed(
                        options.sessionId(),
                        "schematic.undo-too-large"
                ));
                return;
            }
            List<WorldEditSchematicBridge.BlockSnapshot> snapshots = worldEditBridge.captureRegion(
                    world,
                    normalizedOrigin,
                    metadata
            );
            sessionUndo.store(options.sessionId(), world.getName(), snapshots);
            Path schematicFile = definition.directory().resolve(definition.settings().file);

            pasteQueue.submit(() -> {
                try {
                    return worldEditBridge.paste(
                            schematicFile,
                            definition.settings(),
                            metadata,
                            normalizedOrigin,
                            ignoreAir
                    );
                } catch (Exception exception) {
                    return WorldEditSchematicBridge.PasteOutcome.failed(exception.getMessage());
                }
            }).thenAccept(outcome -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (!outcome.success()) {
                    sessionUndo.remove(options.sessionId());
                    future.complete(SchematicPasteResult.failed(
                            options.sessionId(),
                            "schematic.paste-failed"
                    ));
                    return;
                }
                worldEditBridge.clearMarker(world, normalizedOrigin, metadata, definition.settings());
                future.complete(new SchematicPasteResult(
                        options.sessionId(),
                        true,
                        normalizedOrigin.clone(),
                        chestAnchor.clone(),
                        outcome.blockCount(),
                        Optional.empty()
                ));
            }));
        });
        return future;
    }

    @Override
    public CompletableFuture<Void> undo(UUID sessionId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        SchematicSessionUndo.SessionUndo undo = sessionUndo.remove(sessionId);
        if (undo == null || undo.snapshots().isEmpty()) {
            future.complete(null);
            return future;
        }
        World world = Bukkit.getWorld(undo.worldName());
        if (world == null) {
            future.complete(null);
            return future;
        }
        List<WorldEditSchematicBridge.BlockSnapshot> snapshots = undo.snapshots();
        pasteQueue.submit(() -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                worldEditBridge.restoreSnapshots(world, snapshots);
                future.complete(null);
            });
            return null;
        });
        return future;
    }

    @Override
    public void reload() {
        catalog.reload();
    }

    public SchematicCatalog catalog() {
        return catalog;
    }

    public void shutdown() {
        pasteQueue.shutdown();
        catalog.shutdown();
        sessionUndo.clear();
    }

    private static Location normalizePasteOrigin(Location pasteOrigin) {
        Location normalized = pasteOrigin.clone();
        normalized.setX(normalized.getBlockX());
        normalized.setY(normalized.getBlockY());
        normalized.setZ(normalized.getBlockZ());
        return normalized;
    }

    private static int undoBlockCount(SchematicDefinition.SchematicMetadata metadata) {
        int sizeX = metadata.regionMaxX() - metadata.regionMinX() + 1;
        int sizeY = metadata.regionMaxY() - metadata.regionMinY() + 1;
        int sizeZ = metadata.regionMaxZ() - metadata.regionMinZ() + 1;
        long volume = (long) sizeX * sizeY * sizeZ;
        return volume > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) volume;
    }
}
