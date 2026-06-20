package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SchematicMountainSlopeSupport {

    private static final int[][] CARDINAL = {
            {1, 0},
            {-1, 0},
            {0, 1},
            {0, -1}
    };

    private SchematicMountainSlopeSupport() {
    }

    static String validate(
            World world,
            int pasteX,
            int pasteZ,
            List<SchematicFloorColumn> floorColumns,
            SchematicPlacementSettings placement
    ) {
        int minRiseSteps = Math.max(0, placement.minOutwardMountainRiseSteps);
        if (minRiseSteps <= 1 || floorColumns.isEmpty()) {
            return null;
        }
        int scanDepth = resolveScanDepth(placement);
        Set<Long> floorKeys = new HashSet<>();
        for (SchematicFloorColumn column : floorColumns) {
            floorKeys.add(SchematicFloorSupport.columnKey(column.dx(), column.dz()));
        }
        for (SchematicFloorColumn edge : SchematicFloorSupport.perimeterFloorColumns(floorColumns)) {
            for (int[] direction : CARDINAL) {
                int neighborDx = edge.dx() + direction[0];
                int neighborDz = edge.dz() + direction[1];
                if (floorKeys.contains(SchematicFloorSupport.columnKey(neighborDx, neighborDz))) {
                    continue;
                }
                String issue = scanOutwardRay(
                        world,
                        pasteX,
                        pasteZ,
                        edge.dx(),
                        edge.dz(),
                        direction[0],
                        direction[1],
                        scanDepth,
                        minRiseSteps
                );
                if (issue != null) {
                    return issue;
                }
            }
        }
        return null;
    }

    private static int resolveScanDepth(SchematicPlacementSettings placement) {
        if (placement.mountainSlopeScanDepth > 0) {
            return placement.mountainSlopeScanDepth;
        }
        return Math.max(6, Math.max(0, placement.minCliffClearanceFromEdge));
    }

    private static String scanOutwardRay(
            World world,
            int pasteX,
            int pasteZ,
            int startDx,
            int startDz,
            int dirX,
            int dirZ,
            int scanDepth,
            int minRiseSteps
    ) {
        int prevY = NaturalSurfaceResolver.spawnSurfaceY(world, pasteX + startDx, pasteZ + startDz);
        int riseStreak = 0;
        for (int step = 1; step <= scanDepth; step++) {
            int x = pasteX + startDx + dirX * step;
            int z = pasteZ + startDz + dirZ * step;
            int y = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
            if (y > prevY) {
                riseStreak++;
                if (riseStreak >= minRiseSteps) {
                    return "mountain-slope-near-edge steps=" + riseStreak
                            + " need=" + minRiseSteps
                            + " from=" + (pasteX + startDx) + "," + prevY + "," + (pasteZ + startDz)
                            + " at=" + x + "," + y + "," + z;
                }
            } else {
                riseStreak = 0;
            }
            prevY = y;
        }
        return null;
    }
}
