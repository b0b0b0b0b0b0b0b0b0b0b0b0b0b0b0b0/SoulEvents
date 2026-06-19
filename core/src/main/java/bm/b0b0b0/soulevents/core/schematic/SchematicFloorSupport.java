package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SchematicFloorSupport {

    private SchematicFloorSupport() {
    }

    public static List<FlatSurfaceOffset> toFootprint(List<SchematicFloorColumn> floorColumns) {
        List<FlatSurfaceOffset> footprint = new ArrayList<>(floorColumns.size());
        for (SchematicFloorColumn column : floorColumns) {
            footprint.add(new FlatSurfaceOffset(column.dx(), column.dz()));
        }
        return List.copyOf(footprint);
    }

    public static List<SchematicFloorColumn> adaptColumns(
            List<SchematicFloorColumn> floorColumns,
            int approachRing
    ) {
        if (floorColumns.isEmpty()) {
            return List.of();
        }
        List<SchematicFloorColumn> columns = new ArrayList<>(perimeterFloorColumns(floorColumns));
        for (SchematicApproachColumn column : approachRingColumns(floorColumns, approachRing)) {
            columns.add(new SchematicFloorColumn(column.dx(), column.dz(), column.edgeReferenceDy()));
        }
        return List.copyOf(columns);
    }

    public static List<SchematicFloorColumn> perimeterFloorColumns(List<SchematicFloorColumn> floorColumns) {
        if (floorColumns.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> floorDyByKey = new HashMap<>();
        for (SchematicFloorColumn column : floorColumns) {
            floorDyByKey.put(columnKey(column.dx(), column.dz()), column.floorDy());
        }
        List<SchematicFloorColumn> perimeter = new ArrayList<>();
        for (SchematicFloorColumn column : floorColumns) {
            if (isPerimeter(column.dx(), column.dz(), floorDyByKey)) {
                perimeter.add(column);
            }
        }
        return List.copyOf(perimeter);
    }

    public static List<SchematicApproachColumn> approachRingColumns(
            List<SchematicFloorColumn> floorColumns,
            int ringDepth
    ) {
        if (ringDepth <= 0 || floorColumns.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> floorDyByKey = new HashMap<>();
        for (SchematicFloorColumn column : floorColumns) {
            floorDyByKey.put(columnKey(column.dx(), column.dz()), column.floorDy());
        }
        List<SchematicFloorColumn> perimeterFloor = perimeterFloorColumns(floorColumns);
        if (perimeterFloor.isEmpty()) {
            return List.of();
        }

        Set<Long> occupied = new HashSet<>(floorDyByKey.keySet());
        List<SchematicApproachColumn> result = new ArrayList<>();
        Set<Long> frontier = new HashSet<>();
        for (SchematicFloorColumn column : perimeterFloor) {
            collectExteriorNeighbors(column.dx(), column.dz(), occupied, frontier);
        }

        for (int ring = 1; ring <= ringDepth; ring++) {
            if (frontier.isEmpty()) {
                break;
            }
            Set<Long> nextFrontier = new HashSet<>();
            for (long key : frontier) {
                int dx = decodeDx(key);
                int dz = decodeDz(key);
                int edgeReferenceDy = nearestEdgeReferenceDy(dx, dz, perimeterFloor);
                if (edgeReferenceDy == Integer.MIN_VALUE) {
                    continue;
                }
                result.add(new SchematicApproachColumn(dx, dz, ring, edgeReferenceDy));
                occupied.add(key);
                collectExteriorNeighbors(dx, dz, occupied, nextFrontier);
            }
            frontier = nextFrontier;
        }

        return List.copyOf(result);
    }

    private static int nearestEdgeReferenceDy(int dx, int dz, List<SchematicFloorColumn> perimeterFloor) {
        int bestDy = Integer.MIN_VALUE;
        int bestDistance = Integer.MAX_VALUE;
        for (SchematicFloorColumn column : perimeterFloor) {
            int distance = Math.max(Math.abs(dx - column.dx()), Math.abs(dz - column.dz()));
            if (distance > bestDistance) {
                continue;
            }
            if (distance < bestDistance || column.floorDy() > bestDy) {
                bestDistance = distance;
                bestDy = column.floorDy();
            }
        }
        return bestDy;
    }

    private static boolean isPerimeter(int dx, int dz, Map<Long, Integer> floor) {
        return !floor.containsKey(columnKey(dx + 1, dz))
                || !floor.containsKey(columnKey(dx - 1, dz))
                || !floor.containsKey(columnKey(dx, dz + 1))
                || !floor.containsKey(columnKey(dx, dz - 1));
    }

    private static void collectExteriorNeighbors(int dx, int dz, Set<Long> occupied, Set<Long> frontier) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                long key = columnKey(dx + ox, dz + oz);
                if (!occupied.contains(key)) {
                    frontier.add(key);
                }
            }
        }
    }

    private static long columnKey(int dx, int dz) {
        return ((long) dx << 32) | (dz & 0xFFFFFFFFL);
    }

    private static int decodeDx(long key) {
        return (int) (key >> 32);
    }

    private static int decodeDz(long key) {
        return (int) key;
    }

    public static int minFloorDy(
            List<SchematicFloorColumn> floorColumns,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        return floorColumns.stream()
                .mapToInt(SchematicFloorColumn::floorDy)
                .min()
                .orElse(metadata.regionMinY() - metadata.originY());
    }

    public static SchematicWorldBounds toFloorWorldBounds(
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata
    ) {
        List<SchematicFloorColumn> floorColumns = metadata.floorColumns();
        if (floorColumns.isEmpty()) {
            return toRegionWorldBounds(pasteOrigin, metadata);
        }
        int baseX = pasteOrigin.getBlockX();
        int baseY = pasteOrigin.getBlockY();
        int baseZ = pasteOrigin.getBlockZ();
        int minDx = floorColumns.stream().mapToInt(SchematicFloorColumn::dx).min().orElseThrow();
        int maxDx = floorColumns.stream().mapToInt(SchematicFloorColumn::dx).max().orElseThrow();
        int minDz = floorColumns.stream().mapToInt(SchematicFloorColumn::dz).min().orElseThrow();
        int maxDz = floorColumns.stream().mapToInt(SchematicFloorColumn::dz).max().orElseThrow();
        int minFloorDy = minFloorDy(floorColumns, metadata);
        int maxDy = metadata.regionMaxY() - metadata.originY();
        return new SchematicWorldBounds(
                baseX + minDx,
                baseY + minFloorDy,
                baseZ + minDz,
                baseX + maxDx,
                baseY + maxDy,
                baseZ + maxDz
        );
    }

    public static SchematicWorldBounds toRegionWorldBounds(
            Location pasteOrigin,
            SchematicDefinition.SchematicMetadata metadata
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
        return new SchematicWorldBounds(
                baseX + minDx,
                baseY + minDy,
                baseZ + minDz,
                baseX + maxDx,
                baseY + maxDy,
                baseZ + maxDz
        );
    }
}
