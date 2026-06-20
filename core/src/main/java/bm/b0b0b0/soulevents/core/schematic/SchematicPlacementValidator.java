package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnRoughness;
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

        List<SchematicFloorColumn> perimeterColumns =
                SchematicFloorSupport.perimeterFloorColumns(floorColumns);
        if (perimeterColumns.isEmpty()) {
            perimeterColumns = floorColumns;
        }

        int[] floorHeights = new int[floorColumns.size()];
        for (int index = 0; index < floorColumns.size(); index++) {
            SchematicFloorColumn column = floorColumns.get(index);
            floorHeights[index] = NaturalSurfaceResolver.spawnSurfaceY(
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
        for (SchematicFloorColumn column : perimeterColumns) {
            int height = heightAtColumn(column, floorColumns, floorHeights);
            if (referenceY - height > roughnessLimit) {
                return SchematicPlacementResolution.rejected(
                        "schematic-terrain-too-rough delta=" + (referenceY - height)
                                + " limit=" + roughnessLimit
                                + " at=" + (pasteOriginBlockX + column.dx()) + ","
                                + (pasteOriginBlockZ + column.dz())
                );
            }
        }

        for (int index = 0; index < floorColumns.size(); index++) {
            SchematicFloorColumn column = floorColumns.get(index);
            int x = pasteOriginBlockX + column.dx();
            int z = pasteOriginBlockZ + column.dz();
            String surfaceIssue = surfaceIssue(world, x, floorHeights[index], z, placement);
            if (surfaceIssue != null) {
                return SchematicPlacementResolution.rejected("schematic-surface-invalid " + surfaceIssue);
            }
        }

        String edgeIssue = SchematicEdgeClearance.validate(
                world,
                pasteOriginBlockX,
                pasteOriginBlockZ,
                floorColumns,
                placement
        );
        if (edgeIssue != null) {
            return SchematicPlacementResolution.rejected("schematic-surface-invalid " + edgeIssue);
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
                floorColumns,
                floorHeights
        );
        if (clearanceIssue != null) {
            return SchematicPlacementResolution.rejected("schematic-clearance-blocked " + clearanceIssue);
        }

        if (placement.terrainAdaptBlocks > 0 || SchematicTerrainAdapter.needsApproachAdapt(placement)) {
            SchematicTerrainAdapter adapter = SchematicTerrainAdapter.from(
                    placement.terrainMaterials,
                    world,
                    pasteOriginBlockX,
                    pasteOriginBlockZ,
                    metadata.floorColumns()
            );
            if (!adapter.canAdaptAll(
                    world,
                    pasteOriginBlockX,
                    pasteY,
                    pasteOriginBlockZ,
                    metadata,
                    placement
            )) {
                return SchematicPlacementResolution.rejected("schematic-terrain-unadaptable");
            }
        }

        return SchematicPlacementResolution.accepted(
                new Location(world, pasteOriginBlockX, pasteY, pasteOriginBlockZ)
        );
    }

    private static int heightAtColumn(
            SchematicFloorColumn target,
            List<SchematicFloorColumn> floorColumns,
            int[] floorHeights
    ) {
        for (int index = 0; index < floorColumns.size(); index++) {
            SchematicFloorColumn column = floorColumns.get(index);
            if (column.dx() == target.dx() && column.dz() == target.dz()) {
                return floorHeights[index];
            }
        }
        return floorHeights[0];
    }

    private static String clearanceIssue(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            List<SchematicFloorColumn> floorColumns,
            int[] floorHeights
    ) {
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int floorWorldY = pasteY + minDy;
        int adaptLimit = Math.max(0, placement.terrainAdaptBlocks);

        for (int index = 0; index < floorColumns.size(); index++) {
            SchematicFloorColumn column = floorColumns.get(index);
            int surfaceY = floorHeights[index];
            int carveTop = Math.min(surfaceY, floorWorldY + adaptLimit);
            for (int dy = minDy + 1; dy <= maxDy; dy++) {
                int worldY = pasteY + dy;
                if (worldY <= carveTop) {
                    continue;
                }
                Block block = world.getBlockAt(pasteX + column.dx(), worldY, pasteZ + column.dz());
                if (!block.isPassable() && !block.isEmpty()
                        && !NaturalSurfaceResolver.isClearableObstruction(block.getType())) {
                    return "block=" + block.getType().name()
                            + " at=" + (pasteX + column.dx()) + "," + worldY + "," + (pasteZ + column.dz());
                }
            }
            for (int air = 0; air < placement.minAirAbove; air++) {
                Block above = world.getBlockAt(
                        pasteX + column.dx(),
                        pasteY + maxDy + 1 + air,
                        pasteZ + column.dz()
                );
                if (!above.isPassable() && !above.isEmpty()
                        && !NaturalSurfaceResolver.isClearableObstruction(above.getType())) {
                    return "no-air block=" + above.getType().name()
                            + " at=" + (pasteX + column.dx()) + "," + (pasteY + maxDy + 1 + air) + ","
                            + (pasteZ + column.dz());
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
        Block surface = world.getBlockAt(x, y, z);
        Material type = surface.getType();
        if (NaturalSurfaceResolver.isClearableObstruction(type)) {
            if (placement.requireSolidBelow) {
                String belowIssue = solidGroundBelow(world, x, y - 1, z);
                if (belowIssue != null) {
                    return belowIssue;
                }
            }
            return null;
        }
        if (placement.rejectLiquids && (surface.isLiquid() || !surface.isSolid())) {
            return "liquid-or-non-solid block=" + type.name() + " at=" + x + "," + y + "," + z;
        }
        if (!isValidSurfaceBlock(surface)) {
            return "bad-surface block=" + type.name() + " at=" + x + "," + y + "," + z;
        }
        if (placement.requireSolidBelow) {
            String belowIssue = solidGroundBelow(world, x, y - 1, z);
            if (belowIssue != null) {
                return belowIssue;
            }
        }
        return null;
    }

    private static String solidGroundBelow(World world, int x, int y, int z) {
        if (y < world.getMinHeight()) {
            return "no-solid-below below-min-height at=" + x + "," + y + "," + z;
        }
        Block below = world.getBlockAt(x, y, z);
        if (below.isLiquid() || !below.isSolid()) {
            return "no-solid-below block=" + below.getType().name() + " at=" + x + "," + y + "," + z;
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
        return SchematicSpawnRoughness.limit(placement.maxSurfaceDelta, placement.terrainAdaptBlocks);
    }
}
