package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;

public final class SchematicPlacementValidator {

    public Optional<Location> resolve(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            SchematicDefinition definition
    ) {
        return resolveDetailed(world, pasteOriginBlockX, pasteOriginBlockZ, definition, null).location();
    }

    public Optional<Location> resolve(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            SchematicDefinition definition,
            SchematicPlacementSettings placementOverride
    ) {
        return resolveDetailed(world, pasteOriginBlockX, pasteOriginBlockZ, definition, placementOverride).location();
    }

    public SchematicPlacementResolution resolveDetailed(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            SchematicDefinition definition,
            SchematicPlacementSettings placementOverride
    ) {
        if (!definition.isReady()) {
            return SchematicPlacementResolution.rejected("schematic-not-ready");
        }
        SchematicDefinition.SchematicMetadata metadata = definition.metadata();
        SchematicPlacementSettings placement = placementOverride != null
                ? placementOverride
                : definition.settings().placement;
        List<SchematicFloorColumn> floorColumns = metadata.floorColumns();
        if (floorColumns.isEmpty()) {
            return SchematicPlacementResolution.rejected("schematic-probe-empty");
        }

        int[] floorHeights = new int[floorColumns.size()];
        for (int index = 0; index < floorColumns.size(); index++) {
            SchematicFloorColumn column = floorColumns.get(index);
            floorHeights[index] = NaturalSurfaceResolver.placementGroundY(
                    world,
                    pasteOriginBlockX + column.dx(),
                    pasteOriginBlockZ + column.dz()
            );
        }

        int referenceY = floorHeights[0];
        for (int height : floorHeights) {
            referenceY = Math.max(referenceY, height);
        }
        int roughnessLimit = spawnRoughnessLimit(placement);
        for (int index = 0; index < floorHeights.length; index++) {
            int height = floorHeights[index];
            if (referenceY - height > roughnessLimit) {
                SchematicFloorColumn column = floorColumns.get(index);
                return SchematicPlacementResolution.rejected(
                        "schematic-terrain-too-rough delta=" + (referenceY - height)
                                + " limit=" + roughnessLimit
                                + " at=" + (pasteOriginBlockX + column.dx()) + "," + (pasteOriginBlockZ + column.dz())
                );
            }
        }

        List<FlatSurfaceOffset> floorFootprint = metadata.footprint();
        List<FlatSurfaceOffset> probe = SchematicPlacementProbeBuilder.build(
                floorFootprint,
                placement.placementProbeStep,
                placement.safetyMargin
        );
        if (probe.isEmpty()) {
            return SchematicPlacementResolution.rejected("schematic-probe-empty");
        }

        int[] surfaceHeights = new int[probe.size()];
        for (int index = 0; index < probe.size(); index++) {
            FlatSurfaceOffset offset = probe.get(index);
            surfaceHeights[index] = NaturalSurfaceResolver.placementGroundY(
                    world,
                    pasteOriginBlockX + offset.dx(),
                    pasteOriginBlockZ + offset.dz()
            );
        }

        for (int index = 0; index < probe.size(); index++) {
            FlatSurfaceOffset offset = probe.get(index);
            int x = pasteOriginBlockX + offset.dx();
            int z = pasteOriginBlockZ + offset.dz();
            String surfaceIssue = surfaceIssue(world, x, surfaceHeights[index], z, placement);
            if (surfaceIssue != null) {
                return SchematicPlacementResolution.rejected("schematic-surface-invalid " + surfaceIssue);
            }
        }

        int pasteY = referenceY + placement.verticalOffset
                - SchematicFloorSupport.minFloorDy(metadata.floorColumns(), metadata);
        String clearanceIssue = clearanceIssue(
                world,
                pasteOriginBlockX,
                pasteY,
                pasteOriginBlockZ,
                metadata,
                placement,
                probe,
                surfaceHeights
        );
        if (clearanceIssue != null) {
            return SchematicPlacementResolution.rejected("schematic-clearance-blocked " + clearanceIssue);
        }

        return SchematicPlacementResolution.accepted(
                new Location(world, pasteOriginBlockX, pasteY, pasteOriginBlockZ)
        );
    }

    private static String clearanceIssue(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            List<FlatSurfaceOffset> probe,
            int[] surfaceHeights
    ) {
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int floorWorldY = pasteY + minDy;
        int adaptLimit = Math.max(placement.maxSurfaceDelta, placement.terrainAdaptBlocks);
        SchematicPlacementProbeBuilder.Bounds footprintBounds =
                SchematicPlacementProbeBuilder.Bounds.of(metadata.footprint());

        for (int index = 0; index < probe.size(); index++) {
            FlatSurfaceOffset offset = probe.get(index);
            if (!SchematicPlacementProbeBuilder.isInteriorFootprint(
                    offset.dx(),
                    offset.dz(),
                    footprintBounds
            )) {
                continue;
            }
            int surfaceY = surfaceHeights[index];
            int carveTop = Math.min(surfaceY, floorWorldY + adaptLimit);
            for (int dy = minDy + 1; dy <= maxDy; dy++) {
                int worldY = pasteY + dy;
                if (worldY <= carveTop) {
                    continue;
                }
                Block block = world.getBlockAt(pasteX + offset.dx(), worldY, pasteZ + offset.dz());
                if (!block.isPassable() && !block.isEmpty()
                        && !NaturalSurfaceResolver.isClearableObstruction(block.getType())) {
                    return "block=" + block.getType().name()
                            + " at=" + (pasteX + offset.dx()) + "," + worldY + "," + (pasteZ + offset.dz());
                }
            }
            for (int air = 0; air < placement.minAirAbove; air++) {
                Block above = world.getBlockAt(
                        pasteX + offset.dx(),
                        pasteY + maxDy + 1 + air,
                        pasteZ + offset.dz()
                );
                if (!above.isPassable() && !above.isEmpty()
                        && !NaturalSurfaceResolver.isClearableObstruction(above.getType())) {
                    return "no-air block=" + above.getType().name()
                            + " at=" + (pasteX + offset.dx()) + "," + (pasteY + maxDy + 1 + air) + "," + (pasteZ + offset.dz());
                }
            }
        }
        return null;
    }

    private static String surfaceIssue(
            World world,
            int x,
            int y,
            int z,
            SchematicPlacementSettings placement
    ) {
        if (NaturalSurfaceResolver.placementGroundY(world, x, z) != y) {
            return "highest-mismatch at=" + x + "," + z;
        }
        Block surface = world.getBlockAt(x, y, z);
        if (placement.rejectLiquids && (surface.isLiquid() || !surface.isSolid())) {
            return "liquid-or-non-solid block=" + surface.getType().name() + " at=" + x + "," + y + "," + z;
        }
        if (NaturalSurfaceResolver.isClearableObstruction(surface.getType())) {
            return null;
        }
        if (!isValidSurfaceBlock(surface)) {
            return "bad-surface block=" + surface.getType().name() + " at=" + x + "," + y + "," + z;
        }
        if (placement.requireSolidBelow) {
            Block below = world.getBlockAt(x, y - 1, z);
            if (below.isLiquid() || !below.isSolid()) {
                return "no-solid-below block=" + below.getType().name() + " at=" + x + "," + (y - 1) + "," + z;
            }
        }
        return null;
    }

    private static boolean isValidSurfaceBlock(Block block) {
        if (block.isLiquid() || !block.isSolid()) {
            return false;
        }
        Material type = block.getType();
        return type != Material.CACTUS
                && type != Material.SWEET_BERRY_BUSH
                && type != Material.POWDER_SNOW
                && !NaturalSurfaceResolver.isVegetationSurface(type);
    }

    private static int spawnRoughnessLimit(SchematicPlacementSettings placement) {
        int delta = Math.max(0, placement.maxSurfaceDelta);
        int adapt = Math.max(0, placement.terrainAdaptBlocks);
        if (adapt <= 0) {
            return delta;
        }
        return Math.max(delta, adapt);
    }
}
