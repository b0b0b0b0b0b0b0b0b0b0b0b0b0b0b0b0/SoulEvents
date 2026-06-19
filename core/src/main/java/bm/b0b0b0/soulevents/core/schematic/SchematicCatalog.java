package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicProfile;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicFileSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public final class SchematicCatalog {

    private static final List<String> SCHEMATIC_EXTENSIONS = List.of(".schem", ".schematic");

    private final Plugin plugin;
    private final SchematicMarkerScanner scanner;
    private final Map<String, SchematicDefinition> definitions = new LinkedHashMap<>();
    private final ExecutorService scanExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SoulEvents-SchematicScan");
        thread.setDaemon(true);
        return thread;
    });

    public SchematicCatalog(Plugin plugin) {
        this.plugin = plugin;
        this.scanner = new SchematicMarkerScanner(plugin);
    }

    public CompletableFuture<Void> reload() {
        Path root = plugin.getDataFolder().toPath().resolve("schematics");
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create schematics folder", exception);
        }

        BundledSchematicInstaller.installMissing(plugin, root);

        Map<String, SchematicDefinition> pending = new LinkedHashMap<>();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::isSchematicFile)
                    .forEach(schematicFile -> registerSchematic(root, schematicFile, pending));
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to scan schematics folder", exception);
        }

        synchronized (definitions) {
            definitions.clear();
            definitions.putAll(pending);
        }

        if (pending.isEmpty()) {
            plugin.getLogger().info("Schematics folder is empty — drop .schem files into plugins/SoulEvents/schematics/");
        }

        if (!SchematicMarkerScanner.isFaweAvailable()) {
            plugin.getLogger().warning("FastAsyncWorldEdit not found — schematic metadata scan and paste disabled.");
            return CompletableFuture.completedFuture(null);
        }

        List<CompletableFuture<Void>> scans = new ArrayList<>();
        for (SchematicDefinition definition : pending.values()) {
            scans.add(CompletableFuture.supplyAsync(() -> scanDefinition(definition), scanExecutor)
                    .thenAccept(metadata -> {
                        if (metadata == null) {
                            return;
                        }
                        synchronized (definitions) {
                            definitions.put(definition.id(), new SchematicDefinition(
                                    definition.id(),
                                    definition.schematicFile(),
                                    definition.configFile(),
                                    definition.settings(),
                                    metadata
                            ));
                        }
                    }));
        }
        return CompletableFuture.allOf(scans.toArray(CompletableFuture[]::new));
    }

    private void registerSchematic(
            Path root,
            Path schematicFile,
            Map<String, SchematicDefinition> pending
    ) {
        String id = schematicId(schematicFile);
        if (id.isBlank()) {
            return;
        }
        Path configFile = root.resolve(id + ".yml");
        SchematicSettings settings = new SchematicSettings();
        SchematicFileSettings fileSettings = new SchematicFileSettings();
        fileSettings.reload(configFile);
        settings.marker = fileSettings.marker;
        pending.put(id, new SchematicDefinition(id, schematicFile, configFile, settings, null));
    }

    private boolean isSchematicFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        for (String extension : SCHEMATIC_EXTENSIONS) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String schematicId(Path schematicFile) {
        String name = schematicFile.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private SchematicDefinition.SchematicMetadata scanDefinition(SchematicDefinition definition) {
        try {
            SchematicDefinition.SchematicMetadata metadata = scanner.scan(
                    definition.schematicFile(),
                    definition.settings()
            );
            if (metadata.markerValidation() != MarkerValidation.OK
                    && metadata.markerValidation() != MarkerValidation.MANUAL) {
                logMarkerFailure(definition, metadata);
                return metadata;
            }
            plugin.getLogger().info(
                    "Schematic '" + definition.id() + "' loaded: "
                            + metadata.sizeX() + "x" + metadata.sizeY() + "x" + metadata.sizeZ()
                            + ", footprint " + metadata.footprint().size()
                            + ", probe " + metadata.surfaceProbe().size()
                            + ", chest offset (" + metadata.chestOffsetX() + ", "
                            + metadata.chestOffsetY() + ", " + metadata.chestOffsetZ() + ")"
                            + (metadata.markerDetected() ? " [marker]" : " [manual offset]")
            );
            return metadata;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to scan schematic " + definition.id(), exception);
            return null;
        }
    }

    private void logMarkerFailure(String id, SchematicDefinition.SchematicMetadata metadata) {
        String block = metadata.markerBlock();
        switch (metadata.markerValidation()) {
            case AMBIGUOUS -> plugin.getLogger().severe(
                    "Schematic '" + id + "' rejected: found " + metadata.markerCount() + " marker blocks ("
                            + block + "), need exactly 1. Paste disabled. Fix: backup world, set a rare marker.block "
                            + "in schematics/" + id + ".yml, leave that block only at the chest anchor in the "
                            + "build, //schem save, /soulevents reload."
            );
            case NOT_FOUND -> plugin.getLogger().severe(
                    "Schematic '" + id + "' rejected: marker block " + block + " not found in .schem. "
                            + "Paste disabled. Place one " + block + " at the chest anchor or set manual chestOffset "
                            + "with marker.autoDetect: false in schematics/" + id + ".yml."
            );
            default -> {
            }
        }
    }

    private void logMarkerFailure(SchematicDefinition definition, SchematicDefinition.SchematicMetadata metadata) {
        logMarkerFailure(definition.id(), metadata);
    }

    public Collection<String> ids() {
        synchronized (definitions) {
            return List.copyOf(definitions.keySet());
        }
    }

    public Optional<SchematicDefinition> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        synchronized (definitions) {
            return Optional.ofNullable(definitions.get(id));
        }
    }

    public Optional<SchematicProfile> profile(String id) {
        return get(id).filter(SchematicDefinition::isReady).map(this::toProfile);
    }

    private SchematicProfile toProfile(SchematicDefinition definition) {
        SchematicDefinition.SchematicMetadata metadata = definition.metadata();
        SchematicSettings settings = definition.settings();
        return new SchematicProfile(
                definition.id(),
                metadata.sizeX(),
                metadata.sizeY(),
                metadata.sizeZ(),
                metadata.chestOffsetX(),
                metadata.chestOffsetY(),
                metadata.chestOffsetZ(),
                settings.placement.verticalOffset,
                settings.placement.maxSurfaceDelta,
                settings.placement.minAirAbove,
                settings.placement.safetyMargin,
                settings.placement.rejectLiquids,
                settings.placement.requireSolidBelow,
                metadata.footprint(),
                metadata.surfaceProbe()
        );
    }

    public void shutdown() {
        scanExecutor.shutdownNow();
    }
}
