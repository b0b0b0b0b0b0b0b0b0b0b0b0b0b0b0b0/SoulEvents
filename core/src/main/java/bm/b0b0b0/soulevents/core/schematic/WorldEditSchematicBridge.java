package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class WorldEditSchematicBridge {

    public static BlockSnapshot snapshotBlock(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        return new BlockSnapshot(x, y, z, block.getBlockData().clone());
    }

    static long snapshotKey(int x, int y, int z) {
        return ((long) x & 0x3FFFFFFL) << 38 | ((long) y & 0xFFF) << 26 | ((long) z & 0x3FFFFFFL);
    }

    static Set<Long> snapshotKeys(List<BlockSnapshot> snapshots) {
        Set<Long> keys = new HashSet<>(snapshots.size());
        for (BlockSnapshot snapshot : snapshots) {
            keys.add(snapshotKey(snapshot.x(), snapshot.y(), snapshot.z()));
        }
        return keys;
    }

    static boolean appendSnapshotIfAbsent(
            World world,
            List<BlockSnapshot> snapshots,
            Set<Long> keys,
            int x,
            int y,
            int z
    ) {
        if (!keys.add(snapshotKey(x, y, z))) {
            return false;
        }
        snapshots.add(snapshotBlock(world, x, y, z));
        return true;
    }

    public List<BlockSnapshot> captureRegion(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            int horizontalMargin,
            int verticalBelow,
            int verticalAbove
    ) {
        List<BlockSnapshot> snapshots = new ArrayList<>();
        SchematicRegionBounds.VolumeBounds bounds = SchematicRegionBounds.captureBounds(
                metadata,
                horizontalMargin,
                verticalBelow,
                verticalAbove
        );
        int baseX = pasteOrigin.getBlockX();
        int baseY = pasteOrigin.getBlockY();
        int baseZ = pasteOrigin.getBlockZ();
        for (int dx = bounds.minDx(); dx <= bounds.maxDx(); dx++) {
            for (int dz = bounds.minDz(); dz <= bounds.maxDz(); dz++) {
                for (int dy = bounds.minDy(); dy <= bounds.maxDy(); dy++) {
                    snapshots.add(snapshotBlock(world, baseX + dx, baseY + dy, baseZ + dz));
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
            org.bukkit.Location pasteOrigin,
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
