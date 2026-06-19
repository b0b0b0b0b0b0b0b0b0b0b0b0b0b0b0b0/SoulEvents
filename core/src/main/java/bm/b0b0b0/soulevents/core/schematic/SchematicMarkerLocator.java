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
        Block expectedBlock = world.getBlockAt(
                expected.getBlockX(),
                expected.getBlockY(),
                expected.getBlockZ()
        );
        if (expectedBlock.getType() == marker) {
            logMarkerUsed(plugin, schematicId, markerSettings.block, expected, "schematic offset");
            return expected;
        }

        List<Location> found = scanPastedRegion(world, pasteOrigin, metadata, marker);
        if (found.size() == 1) {
            Location markerLocation = blockAnchor(found.getFirst());
            logMarkerUsed(plugin, schematicId, markerSettings.block, markerLocation, "pasted region");
            if (!markerLocation.equals(expected)) {
                plugin.getLogger().info(
                        "Schematic '" + schematicId + "': marker offset in .schem ("
                                + metadata.chestOffsetX() + ", "
                                + metadata.chestOffsetY() + ", "
                                + metadata.chestOffsetZ() + ") differs from pasted "
                                + markerSettings.block
                                + " at "
                                + markerLocation.getBlockX() + ", "
                                + markerLocation.getBlockY() + ", "
                                + markerLocation.getBlockZ()
                );
            }
            return markerLocation;
        }
        if (found.isEmpty()) {
            plugin.getLogger().severe(
                    "Schematic '" + schematicId + "': marker " + markerSettings.block
                            + " not found in pasted region. Place exactly one "
                            + markerSettings.block
                            + " in the .schem at the chest anchor, //schem save, /soulevents reload. "
                            + "Fallback chest offset ("
                            + metadata.chestOffsetX() + ", "
                            + metadata.chestOffsetY() + ", "
                            + metadata.chestOffsetZ() + ") from paste "
                            + pasteOrigin.getBlockX() + ", "
                            + pasteOrigin.getBlockY() + ", "
                            + pasteOrigin.getBlockZ()
            );
            return expected;
        }

        Location fromSchematic = found.stream()
                .map(SchematicMarkerLocator::blockAnchor)
                .filter(location -> location.equals(expected))
                .findFirst()
                .orElse(null);
        if (fromSchematic != null) {
            logMarkerUsed(plugin, schematicId, markerSettings.block, fromSchematic, "schematic offset among "
                    + found.size() + " markers");
            return fromSchematic;
        }

        Location closest = found.stream()
                .map(SchematicMarkerLocator::blockAnchor)
                .min(Comparator.comparingDouble(location -> location.distanceSquared(expected)))
                .orElse(expected);
        plugin.getLogger().warning(
                "Schematic '" + schematicId + "': found " + found.size() + " "
                        + markerSettings.block + " blocks in pasted region; using closest to schematic offset at "
                        + closest.getBlockX() + ", "
                        + closest.getBlockY() + ", "
                        + closest.getBlockZ()
        );
        return closest;
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

    private static void logMarkerUsed(
            Plugin plugin,
            String schematicId,
            String markerBlock,
            Location location,
            String source
    ) {
        plugin.getLogger().info(
                "Schematic '" + schematicId + "': chest marker " + markerBlock + " at "
                        + location.getBlockX() + ", "
                        + location.getBlockY() + ", "
                        + location.getBlockZ()
                        + " (" + source + ")"
        );
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
