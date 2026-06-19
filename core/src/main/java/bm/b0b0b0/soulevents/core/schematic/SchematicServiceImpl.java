package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicPasteOptions;
import bm.b0b0b0.soulevents.api.schematic.SchematicPasteResult;
import bm.b0b0b0.soulevents.api.schematic.SchematicProfile;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import bm.b0b0b0.soulevents.core.message.YamlMessageService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SchematicServiceImpl implements SchematicService {

    private final JavaPlugin plugin;
    private final SchematicCatalog catalog;
    private final SchematicPlacementValidator placementValidator;
    private final SchematicSessionUndo sessionUndo;
    private final WorldEditSchematicBridge worldEditBridge;

    public SchematicServiceImpl(JavaPlugin plugin, YamlMessageService messages) {
        this.plugin = plugin;
        this.catalog = new SchematicCatalog(plugin, messages);
        this.placementValidator = new SchematicPlacementValidator();
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
                .map(definition -> definition.metadata().footprint())
                .orElse(List.of(new FlatSurfaceOffset(0, 0)));
    }

    @Override
    public Optional<Location> resolvePasteOrigin(World world, int blockX, int blockZ, String schematicId) {
        return resolvePasteOrigin(world, blockX, blockZ, schematicId, null);
    }

    @Override
    public Optional<Location> resolvePasteOrigin(
            World world,
            int blockX,
            int blockZ,
            String schematicId,
            SchematicSpawnOverrides overrides
    ) {
        return resolvePasteOriginDetailed(world, blockX, blockZ, schematicId, overrides).location();
    }

    @Override
    public bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution resolvePasteOriginDetailed(
            World world,
            int blockX,
            int blockZ,
            String schematicId,
            SchematicSpawnOverrides overrides
    ) {
        Optional<SchematicDefinition> definitionOptional = catalog.get(schematicId);
        if (definitionOptional.isEmpty()) {
            return bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution.rejected("schematic-unknown");
        }
        SchematicDefinition definition = definitionOptional.get();
        SchematicPlacementSettings placement = SchematicSpawnSupport.resolvePlacement(definition.settings(), overrides);
        return placementValidator.resolveDetailed(world, blockX, blockZ, definition, placement).toApi();
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
        return paste(schematicId, pasteOrigin, options, null);
    }

    @Override
    public CompletableFuture<SchematicPasteResult> paste(
            String schematicId,
            Location pasteOrigin,
            SchematicPasteOptions options,
            SchematicSpawnOverrides overrides
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
        SchematicSettings settings = definition.settings();
        SchematicPlacementSettings placement = SchematicSpawnSupport.resolvePlacement(settings, overrides);
        var blend = SchematicSpawnSupport.resolveBlend(settings, overrides);
        SchematicTerrainContext terrainContext = SchematicTerrainContext.from(placement, blend);
        boolean ignoreAir = SchematicSpawnSupport.resolveIgnoreAir(
                settings,
                overrides,
                options.ignoreAirOverride()
        );
        int blocksPerTick = SchematicSpawnSupport.resolveBlocksPerTick(settings, overrides);
        boolean blendEnabled = options.landscapeBlendOverride() != null
                ? options.landscapeBlendOverride()
                : blend.enabled;
        int blendRadius = options.blendRadiusOverride() != null
                ? options.blendRadiusOverride()
                : blend.radius;
        int terrainAdapt = Math.max(0, placement.terrainAdaptBlocks);
        final int effectiveBlendRadius = resolveBlendRadius(blendEnabled, blendRadius, metadata);
        final int horizontalCaptureMargin = blendEnabled ? Math.max(0, effectiveBlendRadius) : 0;

        CompletableFuture<SchematicPasteResult> future = new CompletableFuture<>();
        int captureBlocks = SchematicRegionBounds.estimateUndoCaptureBlockCount(
                metadata,
                horizontalCaptureMargin,
                terrainAdapt,
                blendEnabled ? effectiveBlendRadius : 0
        );
        int undoLimit = settings.paste.maxUndoBlocks;
        if (undoLimit > 0 && captureBlocks > undoLimit) {
            plugin.getLogger().warning(
                    "Schematic undo snapshot too large for '" + schematicId + "': "
                            + captureBlocks + " capture blocks (limit " + undoLimit
                            + ", region " + metadata.sizeX() + "x" + metadata.sizeY() + "x" + metadata.sizeZ()
                            + ", footprint " + metadata.footprint().size()
                            + ", " + metadata.blockCount() + " schematic blocks). "
                            + "Raise paste.maxUndoBlocks in schematics/" + schematicId + ".yml."
            );
            return CompletableFuture.completedFuture(
                    SchematicPasteResult.failed(options.sessionId(), "schematic.undo-too-large")
            );
        }

        SchematicRegionChunkLoader.ensureLoaded(
                world,
                normalizedOrigin,
                metadata,
                horizontalCaptureMargin
        ).whenComplete((ignored, chunkError) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (chunkError != null) {
                plugin.getLogger().log(
                        java.util.logging.Level.WARNING,
                        "Schematic chunk preload failed for '" + schematicId + "'",
                        chunkError
                );
                future.complete(SchematicPasteResult.failed(options.sessionId(), "schematic.paste-failed"));
                return;
            }
            startPasteAfterChunksLoaded(
                    schematicId,
                    definition,
                    world,
                    normalizedOrigin,
                    metadata,
                    settings,
                    placement,
                    terrainContext,
                    ignoreAir,
                    blocksPerTick,
                    blendEnabled,
                    effectiveBlendRadius,
                    horizontalCaptureMargin,
                    terrainAdapt,
                    options,
                    future
            );
        }));
        return future;
    }

    private void startPasteAfterChunksLoaded(
            String schematicId,
            SchematicDefinition definition,
            World world,
            Location normalizedOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicSettings settings,
            SchematicPlacementSettings placement,
            SchematicTerrainContext terrainContext,
            boolean ignoreAir,
            int blocksPerTick,
            boolean blendEnabled,
            int effectiveBlendRadius,
            int horizontalCaptureMargin,
            int terrainAdapt,
            SchematicPasteOptions options,
            CompletableFuture<SchematicPasteResult> future
    ) {
        Location expectedChest = resolveChestAnchor(normalizedOrigin, schematicId).orElse(normalizedOrigin);
        SchematicRegionPreparer.prepareAsync(
                plugin,
                world,
                normalizedOrigin,
                metadata,
                placement,
                terrainContext.terrainAdapter(),
                horizontalCaptureMargin,
                terrainAdapt
        ).whenComplete((prepared, prepareError) -> {
            if (prepareError != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    String reason = prepareError.getMessage();
                    if ("terrain-too-rough".equals(reason)) {
                        future.complete(SchematicPasteResult.failed(
                                options.sessionId(),
                                "schematic.terrain-too-rough"
                        ));
                    } else {
                        plugin.getLogger().log(
                                java.util.logging.Level.WARNING,
                                "Schematic prepare failed for '" + schematicId + "'",
                                prepareError
                        );
                        future.complete(SchematicPasteResult.failed(
                                options.sessionId(),
                                "schematic.paste-failed"
                        ));
                    }
                });
                return;
            }
            sessionUndo.store(options.sessionId(), world.getName(), prepared.snapshots());
            Path schematicFile = definition.schematicFile();
            FaweSchematicPaster.pasteWhenApplied(
                    plugin,
                    schematicFile,
                    metadata,
                    normalizedOrigin,
                    ignoreAir,
                    blocksPerTick
            ).whenComplete((outcome, pasteError) -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (pasteError != null || outcome == null || !outcome.success()) {
                    worldEditBridge.restoreSnapshots(world, prepared.snapshots());
                    sessionUndo.remove(options.sessionId());
                    String detail = pasteError != null
                            ? pasteError.getMessage()
                            : outcome != null ? outcome.error() : "unknown";
                    plugin.getLogger().warning(
                            "Schematic paste failed for '" + schematicId + "' at "
                                    + normalizedOrigin.getBlockX() + ", "
                                    + normalizedOrigin.getBlockY() + ", "
                                    + normalizedOrigin.getBlockZ()
                                    + (detail != null ? ": " + detail : "")
                    );
                    future.complete(SchematicPasteResult.failed(
                            options.sessionId(),
                            "schematic.paste-failed"
                    ));
                    return;
                }
                Location resolvedChest = resolveChestAfterPaste(
                        plugin,
                        schematicId,
                        world,
                        normalizedOrigin,
                        metadata,
                        settings,
                        expectedChest
                );
                List<WorldEditSchematicBridge.BlockSnapshot> undoSnapshots =
                        new ArrayList<>(prepared.snapshots());
                if (blendEnabled && effectiveBlendRadius > 0) {
                    SchematicWorldBounds bounds = toWorldBounds(normalizedOrigin, metadata);
                    java.util.Set<Long> capturedKeys = WorldEditSchematicBridge.snapshotKeys(undoSnapshots);
                    SchematicLandscapeBlender landscapeBlender = terrainContext.landscapeBlender();
                    landscapeBlender.appendPreBlendCapture(
                            world,
                            bounds,
                            effectiveBlendRadius,
                            undoSnapshots,
                            capturedKeys
                    );
                    landscapeBlender.blend(world, bounds, effectiveBlendRadius);
                    sessionUndo.store(options.sessionId(), world.getName(), List.copyOf(undoSnapshots));
                }
                SchematicRegionChunkLoader.refreshForPlayers(
                        world,
                        normalizedOrigin,
                        metadata,
                        horizontalCaptureMargin
                );
                plugin.getLogger().info(
                        "Schematic '" + schematicId + "' pasted at "
                                + normalizedOrigin.getBlockX() + ", "
                                + normalizedOrigin.getBlockY() + ", "
                                + normalizedOrigin.getBlockZ()
                                + " chest="
                                + resolvedChest.getBlockX() + ", "
                                + resolvedChest.getBlockY() + ", "
                                + resolvedChest.getBlockZ()
                                + " (" + outcome.blockCount() + " blocks)"
                );
                future.complete(new SchematicPasteResult(
                        options.sessionId(),
                        true,
                        normalizedOrigin.clone(),
                        resolvedChest.clone(),
                        outcome.blockCount(),
                        Optional.empty()
                ));
            }));
        });
    }

    private static Location resolveChestAfterPaste(
            Plugin plugin,
            String schematicId,
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicSettings settings,
            Location expectedChest
    ) {
        if (metadata.markerValidation() == MarkerValidation.OK) {
            SchematicMarkerLocator.clearMarkerAt(world, expectedChest, settings.marker);
            return blockAnchor(expectedChest);
        }
        Location resolved = SchematicMarkerLocator.resolveChestAfterPaste(
                plugin,
                schematicId,
                world,
                pasteOrigin,
                metadata,
                settings.marker
        );
        SchematicMarkerLocator.clearMarkerAt(world, resolved, settings.marker);
        return resolved;
    }

    private static Location blockAnchor(Location location) {
        Location anchor = location.clone();
        anchor.setX(anchor.getBlockX());
        anchor.setY(anchor.getBlockY());
        anchor.setZ(anchor.getBlockZ());
        return anchor;
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
        Bukkit.getScheduler().runTask(plugin, () -> {
            worldEditBridge.restoreSnapshots(world, snapshots);
            future.complete(null);
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

    private static int resolveBlendRadius(
            boolean blendEnabled,
            int configuredRadius,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        if (!blendEnabled || configuredRadius <= 0) {
            return 0;
        }
        int minHorizontal = Math.min(metadata.sizeX(), metadata.sizeZ());
        if (minHorizontal <= SchematicPlacementProbeBuilder.FULL_FOOTPRINT_PROBE_LIMIT) {
            return configuredRadius;
        }
        int scaled = Math.max(configuredRadius, minHorizontal / 6);
        return Math.min(scaled, 12);
    }
}
