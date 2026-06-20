package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

    public static List<SchematicFloorColumn> footprintAdaptColumns(List<SchematicFloorColumn> floorColumns) {
        if (floorColumns.isEmpty()) {
            return List.of();
        }
        return List.copyOf(floorColumns);
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
        return exteriorRingColumns(floorColumns, 1, ringDepth);
    }

    public static List<SchematicApproachColumn> approachTailColumns(
            List<SchematicFloorColumn> floorColumns,
            int approachRing,
            int tailDepth
    ) {
        if (tailDepth <= 0 || approachRing <= 0) {
            return List.of();
        }
        return exteriorRingColumns(floorColumns, approachRing + 1, approachRing + tailDepth);
    }

    public static List<SchematicApproachColumn> approachFrontColumns(
            List<SchematicFloorColumn> floorColumns,
            int markerDx,
            int markerDz,
            int depth,
            String facingSetting
    ) {
        if (depth <= 0 || floorColumns.isEmpty()) {
            return List.of();
        }
        int[] facing = resolveApproachFrontFacing(floorColumns, markerDx, markerDz, facingSetting);
        int faceDx = facing[0];
        int faceDz = facing[1];
        if (faceDx == 0 && faceDz == 0) {
            return List.of();
        }
        int centerDx = centroidDx(floorColumns);
        int centerDz = centroidDz(floorColumns);
        List<SchematicApproachColumn> result = new ArrayList<>();
        for (SchematicApproachColumn column : exteriorRingColumns(floorColumns, 1, depth)) {
            int relDx = column.dx() - centerDx;
            int relDz = column.dz() - centerDz;
            if (relDx * faceDx + relDz * faceDz > 0) {
                result.add(column);
            }
        }
        return List.copyOf(result);
    }

    public static List<SchematicApproachColumn> approachAdaptColumns(
            List<SchematicFloorColumn> floorColumns,
            SchematicDefinition.SchematicMetadata metadata,
            int approachRing,
            int frontDepth,
            String frontFacing
    ) {
        LinkedHashMap<Long, SchematicApproachColumn> columns = new LinkedHashMap<>();
        for (SchematicApproachColumn column : approachRingColumns(floorColumns, approachRing)) {
            columns.put(columnKey(column.dx(), column.dz()), column);
        }
        for (SchematicApproachColumn column : approachFrontColumns(
                floorColumns,
                metadata.chestOffsetX(),
                metadata.chestOffsetZ(),
                frontDepth,
                frontFacing
        )) {
            columns.putIfAbsent(columnKey(column.dx(), column.dz()), column);
        }
        return List.copyOf(columns.values());
    }

    public static int approachAdaptRingDepth(
            SchematicApproachColumn column,
            int approachRing,
            int frontDepth
    ) {
        return Math.max(Math.max(approachRing, frontDepth), column.ringDistance());
    }

    public static List<SchematicApproachColumn> exteriorTrimColumns(
            List<SchematicFloorColumn> floorColumns,
            SchematicDefinition.SchematicMetadata metadata,
            int approachRing,
            int raggedOutwardDepth,
            int frontDepth,
            String frontFacing
    ) {
        int maxRing = approachRing + Math.max(0, raggedOutwardDepth);
        if (maxRing <= 0 || floorColumns.isEmpty()) {
            return List.of();
        }
        java.util.LinkedHashMap<Long, SchematicApproachColumn> columns = new java.util.LinkedHashMap<>();
        for (SchematicApproachColumn column : approachRingColumns(floorColumns, maxRing)) {
            columns.put(columnKey(column.dx(), column.dz()), column);
        }
        for (SchematicApproachColumn column : approachAdaptColumns(
                floorColumns,
                metadata,
                approachRing,
                frontDepth,
                frontFacing
        )) {
            columns.putIfAbsent(columnKey(column.dx(), column.dz()), column);
        }
        for (SchematicApproachColumn column : regionExteriorColumns(floorColumns, metadata, maxRing)) {
            columns.putIfAbsent(columnKey(column.dx(), column.dz()), column);
        }
        return List.copyOf(columns.values());
    }

    private static List<SchematicApproachColumn> regionExteriorColumns(
            List<SchematicFloorColumn> floorColumns,
            SchematicDefinition.SchematicMetadata metadata,
            int maxRing
    ) {
        List<SchematicFloorColumn> perimeterFloor = perimeterFloorColumns(floorColumns);
        if (perimeterFloor.isEmpty()) {
            return List.of();
        }
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();
        int fallbackFootDy = minFloorDy(floorColumns, metadata);
        java.util.LinkedHashMap<Long, SchematicApproachColumn> result = new java.util.LinkedHashMap<>();

        for (int ring = 1; ring <= maxRing; ring++) {
            int x0 = minDx - ring;
            int x1 = maxDx + ring;
            int z0 = minDz - ring;
            int z1 = maxDz + ring;
            for (int dx = x0; dx <= x1; dx++) {
                addRegionExteriorCell(result, dx, z0, ring, minDx, maxDx, minDz, maxDz, perimeterFloor, fallbackFootDy);
                addRegionExteriorCell(result, dx, z1, ring, minDx, maxDx, minDz, maxDz, perimeterFloor, fallbackFootDy);
            }
            for (int dz = z0 + 1; dz <= z1 - 1; dz++) {
                addRegionExteriorCell(result, x0, dz, ring, minDx, maxDx, minDz, maxDz, perimeterFloor, fallbackFootDy);
                addRegionExteriorCell(result, x1, dz, ring, minDx, maxDx, minDz, maxDz, perimeterFloor, fallbackFootDy);
            }
        }
        return List.copyOf(result.values());
    }

    private static void addRegionExteriorCell(
            java.util.LinkedHashMap<Long, SchematicApproachColumn> result,
            int dx,
            int dz,
            int ring,
            int minDx,
            int maxDx,
            int minDz,
            int maxDz,
            List<SchematicFloorColumn> perimeterFloor,
            int fallbackFootDy
    ) {
        if (dx >= minDx && dx <= maxDx && dz >= minDz && dz <= maxDz) {
            return;
        }
        long key = columnKey(dx, dz);
        if (result.containsKey(key)) {
            return;
        }
        int edgeReferenceDy = nearestEdgeReferenceDy(dx, dz, perimeterFloor);
        if (edgeReferenceDy == Integer.MIN_VALUE) {
            edgeReferenceDy = fallbackFootDy;
        }
        result.put(key, new SchematicApproachColumn(dx, dz, ring, edgeReferenceDy));
    }

    private static int[] resolveApproachFrontFacing(
            List<SchematicFloorColumn> floorColumns,
            int markerDx,
            int markerDz,
            String facingSetting
    ) {
        String mode = facingSetting == null || facingSetting.isBlank()
                ? "AUTO"
                : facingSetting.trim().toUpperCase(Locale.ROOT);
        return switch (mode) {
            case "NORTH" -> new int[] {0, -1};
            case "SOUTH" -> new int[] {0, 1};
            case "EAST" -> new int[] {1, 0};
            case "WEST" -> new int[] {-1, 0};
            default -> autoApproachFrontFacing(floorColumns, markerDx, markerDz);
        };
    }

    private static int[] autoApproachFrontFacing(
            List<SchematicFloorColumn> floorColumns,
            int markerDx,
            int markerDz
    ) {
        int centerDx = centroidDx(floorColumns);
        int centerDz = centroidDz(floorColumns);
        int faceDx = Integer.compare(markerDx, centerDx);
        int faceDz = Integer.compare(markerDz, centerDz);
        if (faceDx != 0 || faceDz != 0) {
            return new int[] {faceDx, faceDz};
        }
        return new int[] {0, 1};
    }

    private static int centroidDx(List<SchematicFloorColumn> floorColumns) {
        return (int) Math.round(floorColumns.stream().mapToInt(SchematicFloorColumn::dx).average().orElse(0.0));
    }

    private static int centroidDz(List<SchematicFloorColumn> floorColumns) {
        return (int) Math.round(floorColumns.stream().mapToInt(SchematicFloorColumn::dz).average().orElse(0.0));
    }

    public static Set<Long> adaptedColumnKeys(
            List<SchematicFloorColumn> floorColumns,
            int approachRing
    ) {
        Set<Long> keys = new HashSet<>();
        for (SchematicFloorColumn column : perimeterFloorColumns(floorColumns)) {
            keys.add(columnKey(column.dx(), column.dz()));
        }
        for (SchematicApproachColumn column : approachRingColumns(floorColumns, approachRing)) {
            keys.add(columnKey(column.dx(), column.dz()));
        }
        return Set.copyOf(keys);
    }

    private static List<SchematicApproachColumn> exteriorRingColumns(
            List<SchematicFloorColumn> floorColumns,
            int minRingInclusive,
            int maxRingInclusive
    ) {
        if (maxRingInclusive < minRingInclusive || floorColumns.isEmpty()) {
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

        for (int ring = 1; ring <= maxRingInclusive; ring++) {
            if (frontier.isEmpty()) {
                break;
            }
            Set<Long> nextFrontier = new HashSet<>();
            for (long key : frontier) {
                int dx = decodeDx(key);
                int dz = decodeDz(key);
                if (ring >= minRingInclusive) {
                    int edgeReferenceDy = nearestEdgeReferenceDy(dx, dz, perimeterFloor);
                    if (edgeReferenceDy != Integer.MIN_VALUE) {
                        result.add(new SchematicApproachColumn(dx, dz, ring, edgeReferenceDy));
                    }
                }
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

    public static int chebyshevDistanceToSolid(int dx, int dz, List<SchematicFloorColumn> floorColumns) {
        int best = Integer.MAX_VALUE;
        for (SchematicFloorColumn column : floorColumns) {
            int distance = Math.max(Math.abs(dx - column.dx()), Math.abs(dz - column.dz()));
            best = Math.min(best, distance);
        }
        return best;
    }

    public static long columnKey(int dx, int dz) {
        return ((long) dx << 32) | (dz & 0xFFFFFFFFL);
    }

    public static List<FlatSurfaceOffset> perimeterFootprint(List<SchematicFloorColumn> floorColumns) {
        return toFootprint(perimeterFloorColumns(floorColumns));
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
