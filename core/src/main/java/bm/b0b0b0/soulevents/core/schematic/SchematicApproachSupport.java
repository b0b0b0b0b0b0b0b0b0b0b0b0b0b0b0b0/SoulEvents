package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.World;

import java.util.List;

final class SchematicApproachSupport {

    static final int TERRACE_DEPTH = 1;
    static final int MAX_APPROACH_RING = 20;

    private SchematicApproachSupport() {
    }

    record PerimeterDropRise(int maxDrop, int maxRise) {
    }

    static int resolveEffectiveApproachRing(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            List<SchematicFloorColumn> floorColumns,
            SchematicPlacementSettings placement
    ) {
        int configured = Math.max(0, placement.terrainApproachRing);
        int front = Math.max(0, placement.terrainApproachFrontDepth);
        if (configured <= 0 && front <= 0) {
            return 0;
        }
        if (floorColumns.isEmpty()) {
            return Math.min(Math.max(configured, front), MAX_APPROACH_RING);
        }
        PerimeterDropRise measured = measurePerimeterDropRise(
                world, pasteX, pasteY, pasteZ, floorColumns, placement
        );
        int needed = Math.max(measured.maxDrop(), measured.maxRise()) + TERRACE_DEPTH;
        return Math.min(Math.max(Math.max(configured, front), needed), MAX_APPROACH_RING);
    }

    static PerimeterDropRise measurePerimeterDropRise(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            List<SchematicFloorColumn> floorColumns,
            SchematicPlacementSettings placement
    ) {
        int configured = Math.max(
                Math.max(0, placement.terrainApproachRing),
                Math.max(0, placement.terrainApproachFrontDepth)
        );
        int cliffBand = Math.max(0, placement.minCliffClearanceFromEdge);
        int scanRings = Math.min(
                MAX_APPROACH_RING,
                Math.max(Math.max(configured, cliffBand), configured + cliffBand)
        );
        if (scanRings <= 0) {
            return new PerimeterDropRise(0, 0);
        }
        int maxDrop = 0;
        int maxRise = 0;
        for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(floorColumns, scanRings)) {
            int edgeWorldY = pasteY + column.edgeReferenceDy();
            int naturalY = NaturalSurfaceResolver.spawnSurfaceY(
                    world,
                    pasteX + column.dx(),
                    pasteZ + column.dz()
            );
            maxDrop = Math.max(maxDrop, edgeWorldY - naturalY);
            maxRise = Math.max(maxRise, naturalY - edgeWorldY);
        }
        return new PerimeterDropRise(Math.max(0, maxDrop), Math.max(0, maxRise));
    }

    static int approachTargetY(int edgeWorldY, int naturalY, int ringDistance, int ringDepth) {
        int terraceDepth = Math.min(TERRACE_DEPTH, ringDepth);
        if (ringDistance <= terraceDepth) {
            return edgeWorldY;
        }
        int stepsOut = ringDistance - terraceDepth;
        if (naturalY <= edgeWorldY) {
            return Math.max(naturalY, edgeWorldY - stepsOut);
        }
        return Math.min(naturalY, edgeWorldY + stepsOut);
    }

    static int approachAdaptLimit(int terrainAdaptBlocks, SchematicPlacementSettings placement, int effectiveRing) {
        int base = Math.max(1, terrainAdaptBlocks);
        int front = Math.max(0, placement.terrainApproachFrontDepth);
        int ring = Math.max(effectiveRing, front);
        return Math.max(base, ring + TERRACE_DEPTH);
    }
}
