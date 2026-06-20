package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicPlacementSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SpawnInlandRefiner {

    record InlandScanResult(
            List<SpawnRingBiomeLocator.LandPoint> points,
            String emptyDetail
    ) {
    }

    private SpawnInlandRefiner() {
    }

    static InlandScanResult scanPrimary(
            World world,
            int anchorX,
            int anchorZ,
            MapSpawnBoundary.Area area,
            int scanRadius,
            int scanStep,
            int maxCandidates,
            VolcanoTypeSettings type,
            SchematicService schematics
    ) {
        ScanPassResult primary = scanPass(
                world,
                anchorX,
                anchorZ,
                area,
                scanRadius,
                scanStep,
                maxCandidates,
                type,
                schematics
        );
        return new InlandScanResult(primary.points(), primary.emptyDetail());
    }

    static InlandScanResult scanWithFallback(
            World world,
            int anchorX,
            int anchorZ,
            MapSpawnBoundary.Area area,
            int scanRadius,
            int scanStep,
            int expandedRadius,
            int expandedStep,
            int maxCandidates,
            VolcanoTypeSettings type,
            SchematicService schematics
    ) {
        ScanPassResult primary = scanPass(
                world,
                anchorX,
                anchorZ,
                area,
                scanRadius,
                scanStep,
                maxCandidates,
                type,
                schematics
        );
        if (!primary.points().isEmpty()) {
            return new InlandScanResult(primary.points(), null);
        }
        ScanPassResult fallback = scanPass(
                world,
                anchorX,
                anchorZ,
                area,
                expandedRadius,
                expandedStep,
                maxCandidates,
                type,
                schematics
        );
        if (!fallback.points().isEmpty()) {
            return new InlandScanResult(fallback.points(), null);
        }
        String detail = fallback.emptyDetail() != null ? fallback.emptyDetail() : primary.emptyDetail();
        return new InlandScanResult(List.of(), detail);
    }

    private record ScanPassResult(
            List<SpawnRingBiomeLocator.LandPoint> points,
            String emptyDetail
    ) {
    }

    private static ScanPassResult scanPass(
            World world,
            int anchorX,
            int anchorZ,
            MapSpawnBoundary.Area area,
            int scanRadius,
            int step,
            int maxCandidates,
            VolcanoTypeSettings type,
            SchematicService schematics
    ) {
        if (maxCandidates <= 0 || scanRadius <= 0 || step <= 0) {
            return new ScanPassResult(List.of(), "scan-empty invalid-params");
        }
        boolean skipWaterBiomes = type.randomSpawn.skipWaterBiomes;
        String schematicId = type.schematicId();
        int seaFloor = world.getSeaLevel() + 2;
        TypeSchematicPlacementSettings placement = type.schematic.placement;

        List<ScoredPoint> scored = new ArrayList<>();
        int gridCells = 0;
        int skipArea = 0;
        int skipBiome = 0;
        int skipSea = 0;
        int skipCenter = 0;
        Map<String, Integer> quickRejects = new LinkedHashMap<>();
        for (int dx = -scanRadius; dx <= scanRadius; dx += step) {
            for (int dz = -scanRadius; dz <= scanRadius; dz += step) {
                gridCells++;
                int x = anchorX + dx;
                int z = anchorZ + dz;
                if (!area.withinSpawnRing(x, z) || !area.containsBlock(x, z)) {
                    skipArea++;
                    continue;
                }
                if (skipWaterBiomes && SpawnSurfaceFilter.isBlockedBiome(world, x, z)) {
                    skipBiome++;
                    continue;
                }
                ensureChunkLoaded(world, x, z);
                if (skipWaterBiomes && SpawnGroundHeights.spawnSurfaceY(world, x, z) <= seaFloor) {
                    skipSea++;
                    continue;
                }
                if (skipWaterBiomes && !SpawnGroundHeights.isSolidSurface(world, x, z)) {
                    skipCenter++;
                    continue;
                }
                String quickReject = SpawnQuickCheck.reject(
                        world,
                        x,
                        z,
                        skipWaterBiomes,
                        placement.maxSurfaceDelta,
                        placement.terrainAdaptBlocks,
                        placement.minCliffClearanceFromEdge,
                        placement.maxCliffDropFromEdge,
                        schematics,
                        schematicId
                );
                if (quickReject != null) {
                    quickRejects.merge(summarizeQuickReject(quickReject), 1, Integer::sum);
                    continue;
                }
                scored.add(new ScoredPoint(x, z, flatnessScore(world, x, z, schematics, schematicId)));
            }
        }
        scored.sort(Comparator.comparingInt(ScoredPoint::flatness).reversed());
        int limit = Math.min(maxCandidates, scored.size());
        List<SpawnRingBiomeLocator.LandPoint> result = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            ScoredPoint point = scored.get(index);
            result.add(new SpawnRingBiomeLocator.LandPoint(point.x(), point.z()));
        }
        if (result.isEmpty()) {
            return new ScanPassResult(List.of(), formatEmptyDetail(
                    scanRadius,
                    step,
                    gridCells,
                    skipArea,
                    skipBiome,
                    skipSea,
                    skipCenter,
                    quickRejects
            ));
        }
        return new ScanPassResult(List.copyOf(result), null);
    }

    private static void ensureChunkLoaded(World world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.getChunkAt(chunkX, chunkZ);
        }
    }

    private static String summarizeQuickReject(String reason) {
        if (reason.startsWith("quick-terrain-too-rough")) {
            return "quick-terrain-too-rough";
        }
        if (reason.startsWith("quick-liquid-perimeter")) {
            return "quick-liquid-perimeter";
        }
        if (reason.startsWith("perimeter-water")) {
            return "perimeter-water";
        }
        if (reason.startsWith("water-")) {
            return reason.contains(" ") ? reason.substring(0, reason.indexOf(' ')) : reason;
        }
        return reason;
    }

    private static String formatEmptyDetail(
            int scanRadius,
            int step,
            int gridCells,
            int skipArea,
            int skipBiome,
            int skipSea,
            int skipCenter,
            Map<String, Integer> quickRejects
    ) {
        StringBuilder builder = new StringBuilder("scan-empty r=").append(scanRadius)
                .append(" step=").append(step)
                .append(" grid=").append(gridCells);
        if (skipArea > 0) {
            builder.append(" skipArea=").append(skipArea);
        }
        if (skipBiome > 0) {
            builder.append(" skipBiome=").append(skipBiome);
        }
        if (skipSea > 0) {
            builder.append(" skipSea=").append(skipSea);
        }
        if (skipCenter > 0) {
            builder.append(" skipCenter=").append(skipCenter);
        }
        if (!quickRejects.isEmpty()) {
            builder.append(" quick=").append(formatTopCounts(quickRejects, 3));
        }
        return builder.toString();
    }

    private static String formatTopCounts(Map<String, Integer> counts, int limit) {
        StringBuilder builder = new StringBuilder();
        int added = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (added > 0) {
                builder.append(',');
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
            added++;
            if (added >= limit) {
                break;
            }
        }
        return builder.toString();
    }

    private static int flatnessScore(
            World world,
            int originX,
            int originZ,
            SchematicService schematics,
            String schematicId
    ) {
        List<FlatSurfaceOffset> samples = schematics.perimeterFootprintSamples(schematicId);
        if (samples.isEmpty()) {
            return 0;
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (FlatSurfaceOffset offset : samples) {
            int y = SpawnGroundHeights.spawnSurfaceY(world, originX + offset.dx(), originZ + offset.dz());
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return 256 - (maxY - minY);
    }

    private record ScoredPoint(int x, int z, int flatness) {
    }
}

