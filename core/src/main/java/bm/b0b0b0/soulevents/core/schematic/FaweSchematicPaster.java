package bm.b0b0b0.soulevents.core.schematic;

import com.fastasyncworldedit.core.Fawe;
import com.fastasyncworldedit.core.limit.FaweLimit;
import com.fastasyncworldedit.core.util.TaskManager;
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
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class FaweSchematicPaster {

    private static final SchematicPasteQueue PASTE_SERIAL = new SchematicPasteQueue();

    private FaweSchematicPaster() {
    }

    public static CompletableFuture<WorldEditSchematicBridge.PasteOutcome> pasteWhenApplied(
            Plugin plugin,
            Path schematicFile,
            SchematicDefinition.SchematicMetadata metadata,
            Location pasteOrigin,
            boolean ignoreAir,
            int blocksPerTick
    ) {
        CompletableFuture<WorldEditSchematicBridge.PasteOutcome> future = new CompletableFuture<>();
        PASTE_SERIAL.submit(() -> {
            TaskManager.taskManager().async(() -> runPaste(plugin, schematicFile, metadata, pasteOrigin, ignoreAir, blocksPerTick, future));
            return null;
        });
        return future;
    }

    private static void runPaste(
            Plugin plugin,
            Path schematicFile,
            SchematicDefinition.SchematicMetadata metadata,
            Location pasteOrigin,
            boolean ignoreAir,
            int blocksPerTick,
            CompletableFuture<WorldEditSchematicBridge.PasteOutcome> future
    ) {
        WorldEditSchematicBridge.PasteOutcome outcome;
        try {
            outcome = executePaste(schematicFile, metadata, pasteOrigin, ignoreAir, blocksPerTick);
        } catch (Exception exception) {
            completeOnMain(plugin, future, WorldEditSchematicBridge.PasteOutcome.failed(exception.getMessage()));
            return;
        }
        if (!outcome.success()) {
            completeOnMain(plugin, future, outcome);
            return;
        }
        Fawe.instance().getQueueHandler().syncWhenFree(() ->
                completeOnMain(plugin, future, outcome)
        );
    }

    private static void completeOnMain(
            Plugin plugin,
            CompletableFuture<WorldEditSchematicBridge.PasteOutcome> future,
            WorldEditSchematicBridge.PasteOutcome outcome
    ) {
        if (future.isDone()) {
            return;
        }
        TaskManager.taskManager().taskNowMain(() -> future.complete(outcome));
    }

    private static WorldEditSchematicBridge.PasteOutcome executePaste(
            Path schematicFile,
            SchematicDefinition.SchematicMetadata metadata,
            Location pasteOrigin,
            boolean ignoreAir,
            int blocksPerTick
    ) throws Exception {
        if (pasteOrigin.getWorld() == null) {
            return WorldEditSchematicBridge.PasteOutcome.failed("World is null");
        }
        ClipboardFormat format = ClipboardFormats.findByFile(schematicFile.toFile());
        if (format == null) {
            return WorldEditSchematicBridge.PasteOutcome.failed("Unknown schematic format");
        }
        try (InputStream input = Files.newInputStream(schematicFile);
             ClipboardReader reader = format.getReader(input);
             EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder()
                     .world(BukkitAdapter.adapt(pasteOrigin.getWorld()))
                     .maxBlocks(-1)
                     .fastMode(blocksPerTick <= 0)
                     .limit(pasteLimit(blocksPerTick))
                     .build()) {
            Clipboard clipboard = reader.read();
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
            return WorldEditSchematicBridge.PasteOutcome.success(metadata.blockCount());
        }
    }

    private static FaweLimit pasteLimit(int blocksPerTick) {
        FaweLimit limit = FaweLimit.MAX.copy();
        if (blocksPerTick <= 0) {
            limit.FAST_PLACEMENT = true;
            limit.SPEED_REDUCTION = 0;
            return limit;
        }
        limit.FAST_PLACEMENT = false;
        limit.SPEED_REDUCTION = Math.max(0, Math.min(95, 100 - blocksPerTick / 20));
        return limit;
    }
}
