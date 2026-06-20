package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.List;

final class SchematicEdgeClearance {

    private SchematicEdgeClearance() {
    }

    static String validate(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            int pasteY,
            List<SchematicFloorColumn> floorColumns,
            SchematicPlacementSettings placement
    ) {
        if (floorColumns.isEmpty()) {
            return null;
        }
        int waterBand = Math.max(0, placement.minWaterClearanceFromEdge);
        int cliffBand = Math.max(0, placement.minCliffClearanceFromEdge);
        int cliffLimit = SchematicEdgeClearance.effectiveCliffDropLimit(placement);
        if (waterBand == 0 && cliffBand == 0) {
            return legacyLiquidBuffer(world, pasteOriginBlockX, pasteOriginBlockZ, floorColumns, placement);
        }
        int maxBand = Math.max(waterBand, cliffBand);
        int depth = Math.max(0, placement.rejectWaterDepthBlocks);
        String mountainIssue = SchematicMountainSlopeSupport.validate(
                world,
                pasteOriginBlockX,
                pasteOriginBlockZ,
                floorColumns,
                placement
        );
        if (mountainIssue != null) {
            return mountainIssue;
        }
        for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(floorColumns, maxBand)) {
            int edgeDistance = column.ringDistance();
            int x = pasteOriginBlockX + column.dx();
            int z = pasteOriginBlockZ + column.dz();
            if (placement.rejectLiquids && edgeDistance <= waterBand) {
                String water = liquidColumnIssue(world, x, z, depth);
                if (water != null) {
                    return "water-near-edge dist=" + edgeDistance + " " + water;
                }
            }
            if (edgeDistance <= cliffBand) {
                int edgeWorldY = pasteY + column.edgeReferenceDy();
                int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
                int delta = Math.abs(surfaceY - edgeWorldY);
                if (delta > cliffLimit) {
                    return "cliff-near-edge dist=" + edgeDistance
                            + " delta=" + delta
                            + " limit=" + cliffLimit
                            + " at=" + x + "," + surfaceY + "," + z;
                }
            }
        }
        return null;
    }

    static int effectiveCliffDropLimit(SchematicPlacementSettings placement) {
        int configured = Math.max(1, placement.maxCliffDropFromEdge);
        int adapt = Math.max(0, placement.terrainAdaptBlocks);
        if (adapt <= 0) {
            return configured;
        }
        return Math.max(configured, adapt);
    }

    private static String legacyLiquidBuffer(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            List<SchematicFloorColumn> floorColumns,
            SchematicPlacementSettings placement
    ) {
        int radius = Math.max(0, placement.rejectWaterWithinHorizontalBlocks);
        if (!placement.rejectLiquids || radius == 0) {
            return null;
        }
        int depth = Math.max(0, placement.rejectWaterDepthBlocks);
        for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(floorColumns, radius)) {
            int x = pasteOriginBlockX + column.dx();
            int z = pasteOriginBlockZ + column.dz();
            String water = liquidColumnIssue(world, x, z, depth);
            if (water != null) {
                return "liquid-nearby " + water;
            }
        }
        return null;
    }

    private static String liquidColumnIssue(World world, int x, int z, int depth) {
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int toY = Math.max(world.getMinHeight(), surfaceY - depth);
        for (int y = surfaceY; y >= toY; y--) {
            Material type = world.getBlockAt(x, y, z).getType();
            if (type == Material.WATER || type == Material.LAVA) {
                return "block=" + type.name() + " at=" + x + "," + y + "," + z;
            }
        }
        return null;
    }
}
