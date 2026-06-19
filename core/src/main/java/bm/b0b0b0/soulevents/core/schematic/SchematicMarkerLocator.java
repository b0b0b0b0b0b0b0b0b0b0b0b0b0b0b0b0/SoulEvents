package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicMarkerSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class SchematicMarkerLocator {

    private SchematicMarkerLocator() {
    }

    static List<Location> resolveChestAnchorsAfterPaste(
            Plugin plugin,
            String schematicId,
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicMarkerSettings markerSettings,
            int requestedCount
    ) {
        Material marker = SchematicMarkerScanner.parseMarkerMaterial(markerSettings.block);
        List<Location> found = scanPastedRegion(world, pasteOrigin, metadata, marker);
        if (found.isEmpty()) {
            Location expected = blockAnchor(expectedChestAnchor(pasteOrigin, metadata));
            plugin.getLogger().severe(
                    "Schematic '" + schematicId + "': marker " + markerSettings.block
                            + " not found in pasted region. Place markers in .schem, //schem save, /soulevents reload. "
                            + "Fallback offset ("
                            + metadata.chestOffsetX() + ", "
                            + metadata.chestOffsetY() + ", "
                            + metadata.chestOffsetZ() + ") from paste "
                            + pasteOrigin.getBlockX() + ", "
                            + pasteOrigin.getBlockY() + ", "
                            + pasteOrigin.getBlockZ()
            );
            return List.of(expected);
        }

        int effectiveCount = Math.min(Math.max(1, requestedCount), found.size());
        if (effectiveCount < requestedCount) {
            plugin.getLogger().info(
                    "Schematic '" + schematicId + "': requested " + requestedCount + " chest marker(s), "
                            + "found " + found.size() + " " + markerSettings.block + " in paste — using "
                            + effectiveCount
            );
        }

        List<Location> anchors = new ArrayList<>(found.size());
        for (Location location : found) {
            anchors.add(blockAnchor(location));
        }
        Collections.shuffle(anchors, ThreadLocalRandom.current());
        List<Location> selected = List.copyOf(anchors.subList(0, effectiveCount));
        for (Location anchor : selected) {
            plugin.getLogger().info(
                    "Schematic '" + schematicId + "': chest marker " + markerSettings.block + " at "
                            + anchor.getBlockX() + ", "
                            + anchor.getBlockY() + ", "
                            + anchor.getBlockZ()
            );
        }
        return selected;
    }

    static int clearAllMarkersInPastedRegion(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicMarkerSettings markerSettings
    ) {
        if (!markerSettings.replaceWithAir) {
            return 0;
        }
        Material marker = SchematicMarkerScanner.parseMarkerMaterial(markerSettings.block);
        List<Location> found = scanPastedRegion(world, pasteOrigin, metadata, marker);
        for (Location location : found) {
            clearMarkerAt(world, blockAnchor(location), marker, true);
        }
        return found.size();
    }

    static void clearMarkersAt(
            World world,
            List<Location> chestAnchors,
            SchematicMarkerSettings markerSettings
    ) {
        Material marker = SchematicMarkerScanner.parseMarkerMaterial(markerSettings.block);
        for (Location anchor : chestAnchors) {
            clearMarkerAt(world, anchor, marker, markerSettings.replaceWithAir);
        }
    }

    static void clearMarkerAt(
            World world,
            Location chestAnchor,
            SchematicMarkerSettings markerSettings
    ) {
        clearMarkerAt(
                world,
                chestAnchor,
                SchematicMarkerScanner.parseMarkerMaterial(markerSettings.block),
                markerSettings.replaceWithAir
        );
    }

    static void clearMarkerAt(World world, Location chestAnchor, Material marker, boolean replaceWithAir) {
        if (!replaceWithAir) {
            return;
        }
        Block block = world.getBlockAt(
                chestAnchor.getBlockX(),
                chestAnchor.getBlockY(),
                chestAnchor.getBlockZ()
        );
        if (block.getType() == marker) {
            block.setType(Material.AIR, false);
        }
    }

    private static List<Location> scanPastedRegion(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            Material marker
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
        List<Location> found = new ArrayList<>();
        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dy = minDy; dy <= maxDy; dy++) {
                for (int dz = minDz; dz <= maxDz; dz++) {
                    Block block = world.getBlockAt(baseX + dx, baseY + dy, baseZ + dz);
                    if (block.getType() == marker) {
                        found.add(block.getLocation());
                    }
                }
            }
        }
        found.sort(Comparator
                .comparingInt((Location location) -> location.getBlockX())
                .thenComparingInt(Location::getBlockY)
                .thenComparingInt(Location::getBlockZ));
        return found;
    }

    private static Location expectedChestAnchor(
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        return new Location(
                pasteOrigin.getWorld(),
                pasteOrigin.getBlockX() + metadata.chestOffsetX(),
                pasteOrigin.getBlockY() + metadata.chestOffsetY(),
                pasteOrigin.getBlockZ() + metadata.chestOffsetZ()
        );
    }

    private static Location blockAnchor(Location location) {
        Location anchor = location.clone();
        anchor.setX(anchor.getBlockX());
        anchor.setY(anchor.getBlockY());
        anchor.setZ(anchor.getBlockZ());
        return anchor;
    }
}
