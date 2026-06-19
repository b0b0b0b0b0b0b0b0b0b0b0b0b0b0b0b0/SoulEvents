package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicMarkerSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class SchematicMarkerLocator {

    private SchematicMarkerLocator() {
    }

    static Location resolveChestAfterPaste(
            Plugin plugin,
            String schematicId,
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicMarkerSettings markerSettings
    ) {
        Material marker = SchematicMarkerScanner.parseMarkerMaterial(markerSettings.block);
        Location expected = blockAnchor(expectedChestAnchor(pasteOrigin, metadata));
        List<Location> found = scanPastedRegion(world, pasteOrigin, metadata, marker);
        if (found.size() == 1) {
            Location markerLocation = blockAnchor(found.getFirst());
            if (markerLocation.distanceSquared(expected) > 4.0) {
                plugin.getLogger().warning(
                        "Schematic '" + schematicId + "': marker "
                                + markerLocation.getBlockX() + ", "
                                + markerLocation.getBlockY() + ", "
                                + markerLocation.getBlockZ()
                                + " is far from expected chest anchor "
                                + expected.getBlockX() + ", "
                                + expected.getBlockY() + ", "
                                + expected.getBlockZ()
                                + "; using expected offset"
                );
                return blockAnchor(expected);
            }
            return markerLocation;
        }
        if (found.isEmpty()) {
            plugin.getLogger().warning(
                    "Schematic '" + schematicId + "': marker " + markerSettings.block
                            + " not found in pasted region; using chest offset ("
                            + metadata.chestOffsetX() + ", "
                            + metadata.chestOffsetY() + ", "
                            + metadata.chestOffsetZ() + ") from "
                            + pasteOrigin.getBlockX() + ", "
                            + pasteOrigin.getBlockY() + ", "
                            + pasteOrigin.getBlockZ()
            );
            return expected;
        }
        Location closest = found.stream()
                .min(Comparator.comparingDouble(location -> location.distanceSquared(expected)))
                .orElse(expected);
        plugin.getLogger().warning(
                "Schematic '" + schematicId + "': found " + found.size() + " marker blocks after paste; "
                        + "using closest to expected chest anchor "
                        + expected.getBlockX() + ", "
                        + expected.getBlockY() + ", "
                        + expected.getBlockZ()
        );
        return blockAnchor(closest);
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
