package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicMarkerOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicMarkerSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class SchematicMarkerScanner {

    private final Plugin plugin;

    public SchematicMarkerScanner(Plugin plugin) {
        this.plugin = plugin;
    }

    public SchematicDefinition.SchematicMetadata scan(Path schematicFile, SchematicSettings settings) throws IOException {
        if (!Files.isRegularFile(schematicFile)) {
            throw new IOException("Schematic file missing: " + schematicFile);
        }
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile.toFile());
        if (format == null) {
            throw new IOException("Unknown schematic format: " + schematicFile);
        }
        try (InputStream input = Files.newInputStream(schematicFile);
             ClipboardReader reader = format.getReader(input)) {
            Clipboard clipboard = reader.read();
            return buildMetadata(clipboard, settings);
        }
    }

    private SchematicDefinition.SchematicMetadata buildMetadata(Clipboard clipboard, SchematicSettings settings) {
        Region region = clipboard.getRegion();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        BlockVector3 origin = clipboard.getOrigin();

        int sizeX = max.x() - min.x() + 1;
        int sizeY = max.y() - min.y() + 1;
        int sizeZ = max.z() - min.z() + 1;

        SchematicMarkerSettings marker = settings.marker;
        String markerBlockName = marker.block.toUpperCase(Locale.ROOT);
        List<SchematicMarkerOffset> markerOffsets;
        int chestOffsetX;
        int chestOffsetY;
        int chestOffsetZ;
        boolean markerDetected;
        MarkerValidation markerValidation;
        int markerCount;
        if (marker.autoDetect) {
            List<BlockVector3> markers = findMarkers(clipboard, region, marker.block);
            markerCount = markers.size();
            markerOffsets = toMarkerOffsets(markers, origin);
            if (markerCount >= 1) {
                BlockVector3 markerPos = markers.getFirst();
                chestOffsetX = markerPos.x() - origin.x();
                chestOffsetY = markerPos.y() - origin.y();
                chestOffsetZ = markerPos.z() - origin.z();
                markerDetected = true;
                markerValidation = MarkerValidation.OK;
            } else {
                chestOffsetX = marker.chestOffsetX;
                chestOffsetY = marker.chestOffsetY;
                chestOffsetZ = marker.chestOffsetZ;
                markerDetected = false;
                markerValidation = MarkerValidation.NOT_FOUND;
                markerOffsets = List.of(new SchematicMarkerOffset(chestOffsetX, chestOffsetY, chestOffsetZ));
            }
        } else {
            markerCount = 1;
            chestOffsetX = marker.chestOffsetX;
            chestOffsetY = marker.chestOffsetY;
            chestOffsetZ = marker.chestOffsetZ;
            markerDetected = false;
            markerValidation = MarkerValidation.MANUAL;
            markerOffsets = List.of(new SchematicMarkerOffset(chestOffsetX, chestOffsetY, chestOffsetZ));
        }

        List<FlatSurfaceOffset> footprint = scanFootprint(clipboard, region, min.y());
        List<FlatSurfaceOffset> surfaceProbe = SchematicPlacementProbeBuilder.build(
                footprint,
                settings.placement.placementProbeStep,
                settings.placement.safetyMargin
        );
        int blockCount = countBlocks(clipboard, region);

        return new SchematicDefinition.SchematicMetadata(
                sizeX,
                sizeY,
                sizeZ,
                origin.x(),
                origin.y(),
                origin.z(),
                min.x(),
                min.y(),
                min.z(),
                max.x(),
                max.y(),
                max.z(),
                chestOffsetX,
                chestOffsetY,
                chestOffsetZ,
                markerDetected,
                markerValidation,
                markerCount,
                markerBlockName,
                List.copyOf(markerOffsets),
                List.copyOf(footprint),
                List.copyOf(surfaceProbe),
                blockCount
        );
    }

    private static List<SchematicMarkerOffset> toMarkerOffsets(List<BlockVector3> markers, BlockVector3 origin) {
        List<SchematicMarkerOffset> offsets = new ArrayList<>(markers.size());
        for (BlockVector3 marker : markers) {
            offsets.add(new SchematicMarkerOffset(
                    marker.x() - origin.x(),
                    marker.y() - origin.y(),
                    marker.z() - origin.z()
            ));
        }
        return offsets;
    }

    private static List<BlockVector3> findMarkers(Clipboard clipboard, Region region, String markerBlock) {
        String normalized = markerBlock.toUpperCase(Locale.ROOT);
        List<BlockVector3> found = new ArrayList<>();
        for (BlockVector3 pos : region) {
            BlockState state = clipboard.getBlock(pos);
            if (state == null || state.getBlockType() == BlockTypes.AIR) {
                continue;
            }
            String typeId = state.getBlockType().id().replace("minecraft:", "").toUpperCase(Locale.ROOT);
            if (typeId.equals(normalized)) {
                found.add(pos);
            }
        }
        return found;
    }

    private static List<FlatSurfaceOffset> scanFootprint(Clipboard clipboard, Region region, int bottomY) {
        Set<String> seen = new HashSet<>();
        List<FlatSurfaceOffset> footprint = new ArrayList<>();
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 origin = clipboard.getOrigin();
        for (BlockVector3 pos : region) {
            if (pos.y() != bottomY) {
                continue;
            }
            BlockState state = clipboard.getBlock(pos);
            if (state == null || state.getBlockType() == BlockTypes.AIR) {
                continue;
            }
            int dx = pos.x() - origin.x();
            int dz = pos.z() - origin.z();
            String key = dx + ":" + dz;
            if (seen.add(key)) {
                footprint.add(new FlatSurfaceOffset(dx, dz));
            }
        }
        if (footprint.isEmpty()) {
            footprint.add(new FlatSurfaceOffset(0, 0));
        }
        return footprint;
    }

    private static int countBlocks(Clipboard clipboard, Region region) {
        int count = 0;
        for (BlockVector3 pos : region) {
            BlockState state = clipboard.getBlock(pos);
            if (state != null && state.getBlockType() != BlockTypes.AIR) {
                count++;
            }
        }
        return count;
    }

    public static boolean isFaweAvailable() {
        org.bukkit.plugin.Plugin fawe = org.bukkit.Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        return fawe != null && fawe.isEnabled();
    }

    @Deprecated
    public static boolean isWorldEditAvailable() {
        return isFaweAvailable();
    }

    public static Logger logger(Plugin plugin) {
        return plugin.getLogger();
    }

    public static Material parseMarkerMaterial(String name) {
        Material material = Material.matchMaterial(name.toUpperCase(Locale.ROOT));
        return material == null ? Material.BEDROCK : material;
    }
}
