package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class WorldEditSchematicBridge {

    public Object loadClipboard(Path schematicFile) throws Exception {
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile.toFile());
        if (format == null) {
            throw new java.io.IOException("Unknown schematic format");
        }
        try (InputStream input = Files.newInputStream(schematicFile);
             ClipboardReader reader = format.getReader(input)) {
            return reader.read();
        }
    }

    public PasteOutcome pasteClipboard(
            Object clipboardHandle,
            SchematicDefinition.SchematicMetadata metadata,
            Location pasteOrigin,
            boolean ignoreAir
    ) {
        World world = pasteOrigin.getWorld();
        if (world == null) {
            return PasteOutcome.failed("World is null");
        }
        if (!(clipboardHandle instanceof Clipboard clipboard)) {
            return PasteOutcome.failed("Invalid clipboard");
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(BukkitAdapter.adapt(world))) {
            BlockVector3 to = BlockVector3.at(
                    pasteOrigin.getBlockX(),
                    pasteOrigin.getBlockY(),
                    pasteOrigin.getBlockZ()
            );
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(to)
                    .ignoreAirBlocks(ignoreAir)
                    .build();
            Operations.complete(operation);
            editSession.flushSession();
            return PasteOutcome.success(metadata.blockCount());
        } catch (Exception exception) {
            return PasteOutcome.failed(exception.getMessage());
        }
    }

    public List<BlockSnapshot> captureRegion(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        int baseX = pasteOrigin.getBlockX();
        int baseY = pasteOrigin.getBlockY();
        int baseZ = pasteOrigin.getBlockZ();
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                for (int dy = minDy; dy <= maxDy; dy++) {
                    Block block = world.getBlockAt(baseX + dx, baseY + dy, baseZ + dz);
                    snapshots.add(new BlockSnapshot(
                            baseX + dx,
                            baseY + dy,
                            baseZ + dz,
                            block.getBlockData().clone()
                    ));
                }
            }
        }
        return snapshots;
    }

    public void restoreSnapshots(World world, List<BlockSnapshot> snapshots) {
        for (BlockSnapshot snapshot : snapshots) {
            Block block = world.getBlockAt(snapshot.x(), snapshot.y(), snapshot.z());
            block.setBlockData(snapshot.data(), false);
        }
    }

    public void clearMarker(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicSettings settings
    ) {
        if (!settings.marker.replaceWithAir) {
            return;
        }
        Material marker = SchematicMarkerScanner.parseMarkerMaterial(settings.marker.block);
        int x = pasteOrigin.getBlockX() + metadata.chestOffsetX();
        int y = pasteOrigin.getBlockY() + metadata.chestOffsetY();
        int z = pasteOrigin.getBlockZ() + metadata.chestOffsetZ();
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == marker) {
            block.setType(Material.AIR, false);
        }
    }

    public record BlockSnapshot(int x, int y, int z, BlockData data) {
    }

    public record PasteOutcome(boolean success, int blockCount, String error) {

        static PasteOutcome success(int blockCount) {
            return new PasteOutcome(true, blockCount, null);
        }

        static PasteOutcome failed(String error) {
            return new PasteOutcome(false, 0, error);
        }
    }
}
