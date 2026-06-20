package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class SchematicPerimeterRaggedTrimmer {

    private static final int MAX_BUMP_BLOCKS = 2;

    private SchematicPerimeterRaggedTrimmer() {
    }

    static int trim(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            SchematicTerrainAdapter terrain
    ) {
        if (!placement.terrainPerimeterRaggedTrim) {
            return 0;
        }
        List<SchematicFloorColumn> floorColumns = metadata.floorColumns();
        if (floorColumns.isEmpty()) {
            return 0;
        }

        int pasteX = pasteOrigin.getBlockX();
        int pasteY = pasteOrigin.getBlockY();
        int pasteZ = pasteOrigin.getBlockZ();
        float density = placement.terrainApproachRaggedDensity;
        int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);

        int approachRing = SchematicApproachSupport.resolveEffectiveApproachRing(
                world, pasteX, pasteY, pasteZ, floorColumns, placement
        );
        int maxRing = resolveExteriorTrimMaxRing(approachRing, placement.terrainPerimeterRaggedOutwardDepth);
        if (maxRing <= 0) {
            return 0;
        }

        Set<Long> footprintKeys = new HashSet<>();
        for (SchematicFloorColumn column : floorColumns) {
            footprintKeys.add(SchematicFloorSupport.columnKey(column.dx(), column.dz()));
        }

        int changed = 0;
        for (SchematicApproachColumn column : SchematicFloorSupport.exteriorTrimColumns(
                floorColumns,
                metadata,
                approachRing,
                placement.terrainPerimeterRaggedOutwardDepth,
                frontDepth,
                placement.approachFrontFacing
        )) {
            if (footprintKeys.contains(SchematicFloorSupport.columnKey(column.dx(), column.dz()))) {
                continue;
            }
            changed += trimExteriorColumn(
                    world,
                    pasteX + column.dx(),
                    pasteZ + column.dz(),
                    column,
                    pasteY,
                    approachRing,
                    frontDepth,
                    terrain,
                    density,
                    pasteX,
                    pasteZ
            );
        }
        return changed;
    }

    private static int resolveExteriorTrimMaxRing(int approachRing, int raggedOutwardDepth) {
        return approachRing + Math.max(0, raggedOutwardDepth);
    }

    static void appendCapture(
            World world,
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement,
            List<WorldEditSchematicBridge.BlockSnapshot> snapshots,
            Set<Long> capturedKeys
    ) {
        if (!placement.terrainPerimeterRaggedTrim) {
            return;
        }
        List<SchematicFloorColumn> floorColumns = metadata.floorColumns();
        if (floorColumns.isEmpty()) {
            return;
        }

        int pasteX = pasteOrigin.getBlockX();
        int pasteY = pasteOrigin.getBlockY();
        int pasteZ = pasteOrigin.getBlockZ();
        int frontDepth = Math.max(0, placement.terrainApproachFrontDepth);
        int approachRing = SchematicApproachSupport.resolveEffectiveApproachRing(
                world, pasteX, pasteY, pasteZ, floorColumns, placement
        );
        int maxRing = resolveExteriorTrimMaxRing(approachRing, placement.terrainPerimeterRaggedOutwardDepth);
        if (maxRing <= 0) {
            return;
        }

        Set<Long> footprintKeys = new HashSet<>();
        for (SchematicFloorColumn column : floorColumns) {
            footprintKeys.add(SchematicFloorSupport.columnKey(column.dx(), column.dz()));
        }

        for (SchematicApproachColumn column : SchematicFloorSupport.exteriorTrimColumns(
                floorColumns,
                metadata,
                approachRing,
                placement.terrainPerimeterRaggedOutwardDepth,
                frontDepth,
                placement.approachFrontFacing
        )) {
            if (footprintKeys.contains(SchematicFloorSupport.columnKey(column.dx(), column.dz()))) {
                continue;
            }
            int x = pasteX + column.dx();
            int z = pasteZ + column.dz();
            int edgeWorldY = pasteY + column.edgeReferenceDy();
            int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
            int ringDepth = SchematicFloorSupport.approachAdaptRingDepth(column, approachRing, frontDepth);
            int targetY = SchematicApproachSupport.approachTargetY(
                    edgeWorldY,
                    surfaceY,
                    column.ringDistance(),
                    ringDepth
            );
            int delta = surfaceY - targetY;
            if (delta < 1) {
                continue;
            }
            int top = Math.min(surfaceY, targetY + MAX_BUMP_BLOCKS);
            for (int y = targetY + 1; y <= top; y++) {
                WorldEditSchematicBridge.appendSnapshotIfAbsent(world, snapshots, capturedKeys, x, y, z);
            }
        }
    }

    private static int trimExteriorColumn(
            World world,
            int x,
            int z,
            SchematicApproachColumn column,
            int pasteY,
            int approachRing,
            int frontDepth,
            SchematicTerrainAdapter terrain,
            float density,
            int seedX,
            int seedZ
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int surfaceY = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        int ringDepth = SchematicFloorSupport.approachAdaptRingDepth(column, approachRing, frontDepth);
        int targetY = SchematicApproachSupport.approachTargetY(
                edgeWorldY,
                surfaceY,
                column.ringDistance(),
                ringDepth
        );
        int delta = surfaceY - targetY;
        if (delta < 1 || delta > MAX_BUMP_BLOCKS) {
            return 0;
        }

        int changed = 0;
        for (int y = surfaceY; y > targetY; y--) {
            if (!SchematicRaggedEdgeSupport.shouldRaggedTrim(x, z, y, seedX, seedZ, density)) {
                continue;
            }
            Block block = world.getBlockAt(x, y, z);
            if (!terrain.isPerimeterRaggableMaterial(block.getType())) {
                break;
            }
            block.setType(Material.AIR, false);
            changed++;
        }
        return changed;
    }
}
