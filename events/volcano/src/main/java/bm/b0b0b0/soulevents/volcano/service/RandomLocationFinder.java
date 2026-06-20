package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoTypeSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.RandomSpawnSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.TypeSchematicPlacementSettings;
import bm.b0b0b0.soulevents.volcano.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.volcano.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.volcano.integration.WorldGuardRegionIndex;
import bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceRequirements;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnRoughness;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RandomLocationFinder {

    private final FlatSurfaceFinder flatSurfaceFinder;
    private final SchematicService schematics;
    private final ActiveSpawnExclusion activeSpawnExclusion;

    public RandomLocationFinder(
            FlatSurfaceFinder flatSurfaceFinder,
            SchematicService schematics,
            ActiveSpawnExclusion activeSpawnExclusion
    ) {
        this.flatSurfaceFinder = flatSurfaceFinder;
        this.schematics = schematics;
        this.activeSpawnExclusion = activeSpawnExclusion;
    }

    public record Candidate(int x, int z, int chunkX, int chunkZ) {
    }

    public record SearchProfile(
            boolean schematic,
            String schematicId,
            int maxAttempts,
            boolean requireFlatSurface,
            boolean skipWaterBiomes,
            int surfaceDeltaLimit,
            int searchTimeoutSeconds,
            int parallelAttempts,
            int loadedChunkCandidateLimit
    ) {
    }

    private record CandidateValidation(Optional<Location> location, String rejectionReason) {
        static CandidateValidation ok(Location location) {
            return new CandidateValidation(Optional.of(location), null);
        }

        static CandidateValidation reject(String reason) {
            return new CandidateValidation(Optional.empty(), reason);
        }
    }

    private static boolean withinSpawnRing(MapSpawnBoundary.Area area, int x, int z) {
        return area.withinSpawnRing(x, z);
    }

    private static void addLoadedChunkCandidates(
            World world,
            MapSpawnBoundary.Area area,
            Random random,
            List<Candidate> candidates,
            RandomSpawnSettings spawn
    ) {
        int limit = Math.max(0, spawn.loadedChunkCandidateLimit);
        if (limit == 0) {
            return;
        }
        for (Chunk chunk : world.getLoadedChunks()) {
            if (candidates.size() >= limit) {
                return;
            }
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            int baseX = chunkX << 4;
            int baseZ = chunkZ << 4;
            for (int sample = 0; sample < 16 && candidates.size() < limit; sample++) {
                int x = baseX + random.nextInt(16);
                int z = baseZ + random.nextInt(16);
                if (!withinSpawnRing(area, x, z) || !area.containsBlock(x, z)) {
                    continue;
                }
                if (spawn.skipWaterBiomes && SpawnSurfaceFilter.waterSurfaceReason(world, x, z, true) != null) {
                    continue;
                }
                candidates.add(new Candidate(x, z, chunkX, chunkZ));
            }
        }
    }

    private static Candidate randomCandidate(MapSpawnBoundary.Area area, Random random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = area.maxRadius() == area.minRadius()
                ? area.minRadius()
                : area.minRadius() + random.nextDouble() * (area.maxRadius() - area.minRadius());
        int x = area.centerX() + (int) Math.round(Math.cos(angle) * distance);
        int z = area.centerZ() + (int) Math.round(Math.sin(angle) * distance);
        return new Candidate(x, z, x >> 4, z >> 4);
    }

    public static List<Candidate> generateCandidates(VolcanoTypeSettings type, World world) {
        RandomSpawnSettings spawn = type.randomSpawn;
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(spawn, world);
        if (area.maxRadius() <= 0 && area.hasBoundary()) {
            return List.of();
        }
        Random random = ThreadLocalRandom.current();
        List<Candidate> candidates = new ArrayList<>(spawn.maxAttempts);
        addLoadedChunkCandidates(world, area, random, candidates, spawn);
        while (candidates.size() < spawn.maxAttempts) {
            Candidate candidate = randomCandidate(area, random);
            if (!area.containsBlock(candidate.x(), candidate.z())) {
                continue;
            }
            candidates.add(candidate);
        }
        return candidates;
    }

    public Optional<Location> validateCandidate(
            World world,
            Candidate candidate,
            VolcanoTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex
    ) {
        return validateCandidateDetailed(world, candidate, type, gate, regionIndex, false).location();
    }

    private CandidateValidation validateCandidateDetailed(
            World world,
            Candidate candidate,
            VolcanoTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            boolean bypassPlayerProximity
    ) {
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(type.randomSpawn, world);
        if (!area.containsBlock(candidate.x(), candidate.z())) {
            return CandidateValidation.reject("outside-map-boundary");
        }
        RandomSpawnSettings spawn = type.randomSpawn;
        String waterSurface = SpawnSurfaceFilter.waterSurfaceReason(
                world,
                candidate.x(),
                candidate.z(),
                !type.usesSchematic() && spawn.skipWaterBiomes
        );
        if (waterSurface != null) {
            return CandidateValidation.reject(waterSurface);
        }
        if (type.usesSchematic()) {
            SchematicSpawnOverrides spawnOverrides = SchematicSpawnOverridesFactory.from(type.schematic);
            SchematicPlacementResolution placement = schematics.resolvePasteOriginDetailed(
                    world,
                    candidate.x(),
                    candidate.z(),
                    type.schematicId(),
                    spawnOverrides
            );
            if (!placement.accepted()) {
                return CandidateValidation.reject(placement.rejectionReason());
            }
            Location location = placement.location().orElseThrow();
            Optional<Location> ventLocation = schematics.resolveChestAnchor(location, type.schematicId());
            Location gateLocation = ventLocation.orElse(location);
            WorldPlacementResult gateResult = gate.checkLocation(gateLocation, regionIndex, bypassPlayerProximity);
            if (!gateResult.allowed()) {
                return CandidateValidation.reject(SpawnSearchDebug.gateReason(
                        gateResult.denial().name(),
                        gateResult.regionName()
                ));
            }
            Optional<String> activeConflict = activeSpawnExclusion.conflict(
                    world,
                    gateLocation,
                    type.randomSpawn
            );
            if (activeConflict.isPresent()) {
                return CandidateValidation.reject(activeConflict.get());
            }
            Optional<String> pasteConflict = activeSpawnExclusion.conflict(
                    world,
                    location,
                    type.randomSpawn
            );
            if (pasteConflict.isPresent()) {
                return CandidateValidation.reject(pasteConflict.get());
            }
            return CandidateValidation.ok(location);
        }
        if (spawn.requireFlatSurface) {
            FlatSurfaceRequirements requirements = new FlatSurfaceRequirements(
                    spawn.flatMaxHeightDelta,
                    spawn.surfaceYOffset,
                    spawn.flatMinAirAbove,
                    true
            );
            String flatIssue = flatSurfaceIssue(
                    world,
                    candidate.x(),
                    candidate.z(),
                    requirements,
                    footprintFor(type)
            );
            if (flatIssue != null) {
                return CandidateValidation.reject(flatIssue);
            }
            Optional<Location> surfaceLocation = flatSurfaceFinder.resolve(
                    world,
                    candidate.x(),
                    candidate.z(),
                    requirements,
                    footprintFor(type)
            );
            if (surfaceLocation.isEmpty()) {
                return CandidateValidation.reject("flat-surface-not-found");
            }
            Location location = surfaceLocation.get();
            WorldPlacementResult gateResult = gate.checkLocation(location, regionIndex);
            if (!gateResult.allowed()) {
                return CandidateValidation.reject(SpawnSearchDebug.gateReason(
                        gateResult.denial().name(),
                        gateResult.regionName()
                ));
            }
            Optional<String> activeConflict = activeSpawnExclusion.conflict(
                    world,
                    location,
                    type.randomSpawn
            );
            if (activeConflict.isPresent()) {
                return CandidateValidation.reject(activeConflict.get());
            }
            return CandidateValidation.ok(location);
        }
        int y = world.getHighestBlockYAt(candidate.x(), candidate.z());
        Block surface = world.getBlockAt(candidate.x(), y, candidate.z());
        if (surface.isLiquid() || !surface.isSolid()) {
            return CandidateValidation.reject("surface-liquid-or-invalid block=" + surface.getType().name());
        }
        Location location = new Location(
                world,
                candidate.x() + 0.5,
                y + spawn.surfaceYOffset,
                candidate.z() + 0.5
        );
        WorldPlacementResult gateResult = gate.checkLocation(location, regionIndex);
        if (!gateResult.allowed()) {
            return CandidateValidation.reject(SpawnSearchDebug.gateReason(
                    gateResult.denial().name(),
                    gateResult.regionName()
            ));
        }
        Optional<String> activeConflict = activeSpawnExclusion.conflict(
                world,
                location,
                type.randomSpawn
        );
        if (activeConflict.isPresent()) {
            return CandidateValidation.reject(activeConflict.get());
        }
        return CandidateValidation.ok(location);
    }

    private static String flatSurfaceIssue(
            World world,
            int blockX,
            int blockZ,
            FlatSurfaceRequirements requirements,
            List<FlatSurfaceOffset> footprint
    ) {
        List<FlatSurfaceOffset> points = footprint == null || footprint.isEmpty()
                ? List.of(new FlatSurfaceOffset(0, 0))
                : footprint;
        int[] surfaceHeights = new int[points.size()];
        for (int index = 0; index < points.size(); index++) {
            FlatSurfaceOffset offset = points.get(index);
            surfaceHeights[index] = world.getHighestBlockYAt(blockX + offset.dx(), blockZ + offset.dz());
        }
        int referenceY = surfaceHeights[0];
        for (int height : surfaceHeights) {
            referenceY = Math.max(referenceY, height);
        }
        for (int index = 0; index < surfaceHeights.length; index++) {
            int height = surfaceHeights[index];
            if (referenceY - height > requirements.maxSurfaceDelta()) {
                FlatSurfaceOffset offset = points.get(index);
                return "flat-terrain-too-rough delta=" + (referenceY - height)
                        + " at=" + (blockX + offset.dx()) + "," + (blockZ + offset.dz());
            }
        }
        for (int index = 0; index < points.size(); index++) {
            FlatSurfaceOffset offset = points.get(index);
            int x = blockX + offset.dx();
            int y = surfaceHeights[index];
            int z = blockZ + offset.dz();
            if (world.getHighestBlockYAt(x, z) != y) {
                return "flat-highest-mismatch at=" + x + "," + z;
            }
            Block surface = world.getBlockAt(x, y, z);
            if (surface.isLiquid() || !surface.isSolid()) {
                return "flat-surface-invalid block=" + surface.getType().name() + " at=" + x + "," + y + "," + z;
            }
            Material type = surface.getType();
            if (type == Material.MAGMA_BLOCK
                    || type == Material.CACTUS
                    || type == Material.SWEET_BERRY_BUSH
                    || type == Material.POWDER_SNOW) {
                return "flat-bad-surface block=" + type.name() + " at=" + x + "," + y + "," + z;
            }
            if (requirements.requireSolidBelow()) {
                Block below = world.getBlockAt(x, y - 1, z);
                if (below.isLiquid() || !below.isSolid()) {
                    return "flat-no-solid-below at=" + x + "," + (y - 1) + "," + z;
                }
            }
            for (int offsetY = 1; offsetY <= requirements.minAirAbove(); offsetY++) {
                Block above = world.getBlockAt(x, y + offsetY, z);
                if (!above.isPassable() && !above.isEmpty()) {
                    return "flat-no-air block=" + above.getType().name()
                            + " at=" + x + "," + (y + offsetY) + "," + z;
                }
            }
        }
        return null;
    }

    public static List<FlatSurfaceOffset> footprintFor(VolcanoTypeSettings type, SchematicService schematics) {
        if (type.usesSchematic()) {
            return schematics.footprint(type.schematicId());
        }
        return List.of(new FlatSurfaceOffset(0, 0));
    }

    public List<FlatSurfaceOffset> footprintFor(VolcanoTypeSettings type) {
        return footprintFor(type, schematics);
    }

    public void findAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            Consumer<Optional<Location>> callback
    ) {
        findAsync(plugin, type, world, gate, null, callback);
    }

    public void findAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        findAsync(plugin, type, world, gate, debug, "scheduler", false, callback);
    }

    public void findAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            SpawnSearchDebug debug,
            String searchSource,
            boolean bypassLimits,
            Consumer<Optional<Location>> callback
    ) {
        RandomSpawnSettings spawn = type.randomSpawn;
        SpawnSearchTuning.Values tuning = SpawnSearchTuning.resolve(searchSource, spawn, bypassLimits);
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(spawn, world);
        int chunkMargin = type.usesSchematic()
                ? spawnChunkMargin(type.schematic.placement)
                : 0;
        SearchProfile profile = new SearchProfile(
                type.usesSchematic(),
                type.schematicId(),
                tuning.maxAttempts(),
                spawn.requireFlatSurface,
                spawn.skipWaterBiomes,
                type.usesSchematic()
                        ? SchematicSpawnRoughness.limit(
                        type.schematic.placement.maxSurfaceDelta,
                        type.schematic.placement.terrainAdaptBlocks)
                        : spawn.flatMaxHeightDelta,
                spawn.searchTimeoutSeconds,
                1,
                spawn.loadedChunkCandidateLimit
        );
        if (debug != null) {
            debug.start(world.getName(), area, profile);
        }
        if (area.maxRadius() <= 0 && area.hasBoundary()) {
            if (debug != null) {
                debug.noCandidates("no valid spawn radius (maxRadius=0 with map boundary)");
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(Optional.empty()));
            return;
        }
        WorldGuardRegionIndex regionIndex = WorldGuardIntegrations.regionIndex(world);
        long deadlineMillis = System.currentTimeMillis() + Math.max(1, spawn.searchTimeoutSeconds) * 1000L;
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicInteger attemptCounter = new AtomicInteger(0);
        Random random = ThreadLocalRandom.current();
        searchNextAsync(
                plugin,
                type,
                world,
                gate,
                regionIndex,
                area,
                chunkMargin,
                random,
                attemptCounter,
                deadlineMillis,
                finished,
                debug,
                tuning,
                callback
        );
    }

    private void searchNextAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            MapSpawnBoundary.Area area,
            int chunkMargin,
            Random random,
            AtomicInteger attemptCounter,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            SpawnSearchTuning.Values tuning,
            Consumer<Optional<Location>> callback
    ) {
        if (finished.get()) {
            return;
        }
        RandomSpawnSettings spawn = type.randomSpawn;
        int maxAttempts = tuning.maxAttempts();
        if (System.currentTimeMillis() >= deadlineMillis) {
            finishSearch(plugin, type, deadlineMillis, finished, debug, callback);
            return;
        }
        int attemptIndex = attemptCounter.getAndIncrement();
        if (attemptIndex >= maxAttempts) {
            finishSearch(plugin, type, deadlineMillis, finished, debug, callback);
            return;
        }
        Optional<SpawnRingBiomeLocator.LandPoint> landPoint = SpawnRingBiomeLocator.locate(world, area, random);
        if (landPoint.isEmpty()) {
            if (debug != null) {
                debug.reject(attemptIndex, maxAttempts, -1, -1, "biome-not-found");
            }
            searchNextAsync(
                    plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                    attemptCounter, deadlineMillis, finished, debug, tuning, callback
            );
            return;
        }
        SpawnRingBiomeLocator.LandPoint anchor = landPoint.get();
        if (spawn.skipWaterBiomes && SpawnSurfaceFilter.isBlockedBiome(world, anchor.x(), anchor.z())) {
            if (debug != null) {
                debug.reject(attemptIndex, maxAttempts, anchor.x(), anchor.z(), "anchor-blocked-biome");
            }
            searchNextAsync(
                    plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                    attemptCounter, deadlineMillis, finished, debug, tuning, callback
            );
            return;
        }
        searchAnchorScanAsync(
                plugin,
                type,
                world,
                gate,
                regionIndex,
                area,
                chunkMargin,
                random,
                attemptCounter,
                deadlineMillis,
                finished,
                debug,
                tuning,
                callback,
                anchor,
                attemptIndex
        );
    }

    private void searchAnchorScanAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            MapSpawnBoundary.Area area,
            int chunkMargin,
            Random random,
            AtomicInteger attemptCounter,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            SpawnSearchTuning.Values tuning,
            Consumer<Optional<Location>> callback,
            SpawnRingBiomeLocator.LandPoint anchor,
            int attemptIndex
    ) {
        RandomSpawnSettings spawn = type.randomSpawn;
        int maxAttempts = tuning.maxAttempts();
        int expandedScanRadius = tuning.scanRadius() + 48;
        int blockRadius = expandedScanRadius
                + spawnChunkMargin(type.schematic.placement)
                + footprintBlockReach(schematics, type.schematicId());
        CompletableFuture<Void> scanChunksLoaded = type.usesSchematic()
                ? schematics.prepareSpawnSearchArea(world, anchor.x(), anchor.z(), blockRadius)
                : world.getChunkAtAsync(anchor.x() >> 4, anchor.z() >> 4, true).thenApply(chunk -> null);
        scanChunksLoaded.whenComplete((ignored, loadError) -> plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    if (finished.get()) {
                        return;
                    }
                    if (loadError != null) {
                        if (debug != null) {
                            debug.reject(
                                    attemptIndex,
                                    maxAttempts,
                                    anchor.x(),
                                    anchor.z(),
                                    "anchor-chunk-load-failed"
                            );
                        }
                        searchNextAsync(
                                plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                                attemptCounter, deadlineMillis, finished, debug, tuning, callback
                        );
                        return;
                    }
                    SpawnInlandRefiner.InlandScanResult inlandScan = SpawnInlandRefiner.scanWithFallback(
                            world,
                            anchor.x(),
                            anchor.z(),
                            area,
                            tuning.scanRadius(),
                            tuning.scanStep(),
                            expandedScanRadius,
                            Math.max(8, tuning.scanStep() - 4),
                            tuning.maxScanCandidates(),
                            type,
                            schematics
                    );
                    List<SpawnRingBiomeLocator.LandPoint> probes = inlandScan.points();
                    if (probes.isEmpty()) {
                        if (debug != null) {
                            String detail = inlandScan.emptyDetail() != null
                                    ? inlandScan.emptyDetail()
                                    : "scan-empty";
                            debug.reject(
                                    attemptIndex,
                                    maxAttempts,
                                    anchor.x(),
                                    anchor.z(),
                                    detail
                            );
                        }
                        searchNextAsync(
                                plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                                attemptCounter, deadlineMillis, finished, debug, tuning, callback
                        );
                        return;
                    }
                    searchAttemptProbesAsync(
                            plugin,
                            type,
                            world,
                            gate,
                            regionIndex,
                            area,
                            chunkMargin,
                            random,
                            attemptCounter,
                            deadlineMillis,
                            finished,
                            debug,
                            tuning,
                            callback,
                            probes,
                            attemptIndex
                    );
                }
        ));
    }

    private void searchAttemptProbesAsync(
            Plugin plugin,
            VolcanoTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            MapSpawnBoundary.Area area,
            int chunkMargin,
            Random random,
            AtomicInteger attemptCounter,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            SpawnSearchTuning.Values tuning,
            Consumer<Optional<Location>> callback,
            List<SpawnRingBiomeLocator.LandPoint> probes,
            int attemptIndex
    ) {
        RandomSpawnSettings spawn = type.randomSpawn;
        int maxAttempts = tuning.maxAttempts();
        List<SpawnRingBiomeLocator.LandPoint> candidates = new ArrayList<>(probes.size());
        for (int probeIndex = 0; probeIndex < probes.size(); probeIndex++) {
            SpawnRingBiomeLocator.LandPoint point = probes.get(probeIndex);
            String prefilter = spawn.skipWaterBiomes
                    ? SpawnLandRefiner.loadedPrefilterReason(world, point.x(), point.z(), true)
                    : null;
            if (prefilter != null) {
                if (debug != null) {
                    debug.reject(
                            attemptIndex,
                            maxAttempts,
                            point.x(),
                            point.z(),
                            probeLabel(probeIndex, probes.size()) + prefilter
                    );
                }
                continue;
            }
            candidates.add(point);
        }
        if (candidates.isEmpty()) {
            searchNextAsync(
                    plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                    attemptCounter, deadlineMillis, finished, debug, tuning, callback
            );
            return;
        }
        List<int[]> blockOrigins = new ArrayList<>(candidates.size());
        for (SpawnRingBiomeLocator.LandPoint point : candidates) {
            blockOrigins.add(new int[] {point.x(), point.z()});
        }
        CompletableFuture<Void> chunksLoaded = type.usesSchematic()
                ? schematics.prepareSpawnSearchFootprint(
                world, type.schematicId(), chunkMargin, blockOrigins)
                : world.getChunkAtAsync(candidates.getFirst().x() >> 4, candidates.getFirst().z() >> 4, true)
                .thenApply(chunk -> null);
        chunksLoaded.whenComplete((ignored, loadError) -> plugin.getServer().getScheduler().runTask(
                plugin,
                () -> {
                    if (finished.get()) {
                        return;
                    }
                    if (loadError != null) {
                        if (debug != null) {
                            debug.reject(
                                    attemptIndex,
                                    maxAttempts,
                                    candidates.getFirst().x(),
                                    candidates.getFirst().z(),
                                    "chunk-load-failed"
                            );
                        }
                        searchNextAsync(
                                plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                                attemptCounter, deadlineMillis, finished, debug, tuning, callback
                        );
                        return;
                    }
                    for (int probeIndex = 0; probeIndex < candidates.size(); probeIndex++) {
                        SpawnRingBiomeLocator.LandPoint point = candidates.get(probeIndex);
                        Candidate candidate = new Candidate(point.x(), point.z(), point.x() >> 4, point.z() >> 4);
                        CandidateValidation validation = validateCandidateDetailed(
                                world,
                                candidate,
                                type,
                                gate,
                                regionIndex,
                                tuning.bypassPlayerProximity()
                        );
                        if (validation.location().isPresent()) {
                            if (!finished.compareAndSet(false, true)) {
                                return;
                            }
                            Location location = validation.location().get();
                            if (debug != null) {
                                debug.success(
                                        attemptIndex,
                                        maxAttempts,
                                        candidate.x(),
                                        location.getBlockY(),
                                        candidate.z()
                                );
                            }
                            callback.accept(validation.location());
                            return;
                        }
                        if (debug != null) {
                            debug.reject(
                                    attemptIndex,
                                    maxAttempts,
                                    candidate.x(),
                                    candidate.z(),
                                    probeLabel(probeIndex, probes.size()) + validation.rejectionReason()
                            );
                        }
                    }
                    searchNextAsync(
                            plugin, type, world, gate, regionIndex, area, chunkMargin, random,
                            attemptCounter, deadlineMillis, finished, debug, tuning, callback
                    );
                }
        ));
    }

    private static String probeLabel(int probeIndex, int probeTotal) {
        if (probeTotal <= 1) {
            return "";
        }
        return "probe " + (probeIndex + 1) + "/" + probeTotal + " ";
    }

    private void finishSearch(
            Plugin plugin,
            VolcanoTypeSettings type,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            if (debug != null) {
                if (System.currentTimeMillis() >= deadlineMillis) {
                    debug.finishTimedOut(type.randomSpawn.searchTimeoutSeconds);
                } else {
                    debug.finishFailed();
                }
            }
            callback.accept(Optional.empty());
        });
    }

    public Optional<Location> find(VolcanoTypeSettings type, World world, WorldPlacementGate gate) {
        WorldGuardRegionIndex regionIndex = WorldGuardIntegrations.regionIndex(world);
        RandomSpawnSettings spawn = type.randomSpawn;
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(spawn, world);
        int chunkMargin = type.usesSchematic()
                ? spawnChunkMargin(type.schematic.placement)
                : 0;
        Random random = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < spawn.maxAttempts; attempt++) {
            Optional<SpawnRingBiomeLocator.LandPoint> landPoint = SpawnRingBiomeLocator.locate(world, area, random);
            if (landPoint.isEmpty()) {
                continue;
            }
            SpawnRingBiomeLocator.LandPoint anchor = landPoint.get();
            SpawnSearchTuning.Values tuning = SpawnSearchTuning.resolve("scheduler", spawn, false);
            int expandedScanRadius = tuning.scanRadius() + 48;
            int blockRadius = expandedScanRadius
                    + spawnChunkMargin(type.schematic.placement)
                    + footprintBlockReach(schematics, type.schematicId());
            if (type.usesSchematic()) {
                schematics.prepareSpawnSearchArea(
                        world,
                        anchor.x(),
                        anchor.z(),
                        blockRadius
                ).join();
            } else if (!world.isChunkLoaded(anchor.x() >> 4, anchor.z() >> 4)) {
                world.loadChunk(anchor.x() >> 4, anchor.z() >> 4);
            }
            SpawnInlandRefiner.InlandScanResult inlandScan = SpawnInlandRefiner.scanWithFallback(
                    world,
                    anchor.x(),
                    anchor.z(),
                    area,
                    tuning.scanRadius(),
                    tuning.scanStep(),
                    expandedScanRadius,
                    Math.max(8, tuning.scanStep() - 4),
                    tuning.maxScanCandidates(),
                    type,
                    schematics
            );
            List<SpawnRingBiomeLocator.LandPoint> candidates = inlandScan.points();
            if (candidates.isEmpty()) {
                continue;
            }
            for (SpawnRingBiomeLocator.LandPoint point : candidates) {
                Candidate candidate = new Candidate(point.x(), point.z(), point.x() >> 4, point.z() >> 4);
                Optional<Location> location = validateCandidate(world, candidate, type, gate, regionIndex);
                if (location.isPresent()) {
                    return location;
                }
            }
        }
        return Optional.empty();
    }

    public Optional<Location> findInWorlds(VolcanoTypeSettings type, WorldPlacementGate gate, List<String> worldNames) {
        for (String worldName : worldNames) {
            World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            Optional<Location> location = find(type, world, gate);
            if (location.isPresent()) {
                return location;
            }
        }
        return Optional.empty();
    }

    private static int footprintBlockReach(SchematicService schematics, String schematicId) {
        int reach = 0;
        for (FlatSurfaceOffset offset : schematics.perimeterFootprintSamples(schematicId)) {
            reach = Math.max(reach, Math.max(Math.abs(offset.dx()), Math.abs(offset.dz())));
        }
        return reach + 8;
    }

    private static int spawnChunkMargin(TypeSchematicPlacementSettings placement) {
        return Math.max(
                placement.safetyMargin,
                Math.max(
                        placement.rejectWaterWithinHorizontalBlocks,
                        Math.max(
                                placement.minWaterClearanceFromEdge,
                                Math.max(
                                        placement.minCliffClearanceFromEdge,
                                        Math.max(placement.terrainApproachRing, placement.terrainApproachFrontDepth) + 2
                                )
                        )
                )
        );
    }
}

