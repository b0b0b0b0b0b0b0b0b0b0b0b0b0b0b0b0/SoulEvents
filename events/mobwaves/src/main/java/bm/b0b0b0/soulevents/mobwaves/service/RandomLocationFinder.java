package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeTypeSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeRandomSpawnSettings;
import bm.b0b0b0.soulevents.mobwaves.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.mobwaves.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.mobwaves.integration.WorldGuardRegionIndex;
import bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.schematic.SchematicSpawnOverrides;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceRequirements;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
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
        long dx = (long) x - area.centerX();
        long dz = (long) z - area.centerZ();
        long minSquared = (long) area.minRadius() * area.minRadius();
        long maxSquared = (long) area.maxRadius() * area.maxRadius();
        long distanceSquared = dx * dx + dz * dz;
        return distanceSquared >= minSquared && distanceSquared <= maxSquared;
    }

    private static void addLoadedChunkCandidates(
            World world,
            MapSpawnBoundary.Area area,
            Random random,
            List<Candidate> candidates,
            HordeRandomSpawnSettings spawn
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

    public static List<Candidate> generateCandidates(HordeTypeSettings type, World world) {
        HordeRandomSpawnSettings spawn = type.randomSpawn;
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(spawn, world);
        if (area.maxRadius() <= 0 && area.hasBoundary()) {
            return List.of();
        }
        Random random = ThreadLocalRandom.current();
        List<Candidate> candidates = new ArrayList<>(spawn.maxAttempts);
        addLoadedChunkCandidates(world, area, random, candidates, spawn);
        while (candidates.size() < spawn.maxAttempts) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = area.maxRadius() == area.minRadius()
                    ? area.minRadius()
                    : area.minRadius() + random.nextDouble() * (area.maxRadius() - area.minRadius());
            int x = area.centerX() + (int) Math.round(Math.cos(angle) * distance);
            int z = area.centerZ() + (int) Math.round(Math.sin(angle) * distance);
            if (!area.containsBlock(x, z)) {
                continue;
            }
            candidates.add(new Candidate(x, z, x >> 4, z >> 4));
        }
        return candidates;
    }

    public Optional<Location> validateCandidate(
            World world,
            Candidate candidate,
            HordeTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex
    ) {
        return validateCandidateDetailed(world, candidate, type, gate, regionIndex).location();
    }

    private CandidateValidation validateCandidateDetailed(
            World world,
            Candidate candidate,
            HordeTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex
    ) {
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(type.randomSpawn, world);
        if (!area.containsBlock(candidate.x(), candidate.z())) {
            return CandidateValidation.reject("outside-map-boundary");
        }
        HordeRandomSpawnSettings spawn = type.randomSpawn;
        String waterSurface = SpawnSurfaceFilter.waterSurfaceReason(
                world,
                candidate.x(),
                candidate.z(),
                spawn.skipWaterBiomes
        );
        if (waterSurface != null) {
            return CandidateValidation.reject(waterSurface);
        }
        if (type.usesSchematic()) {
            SchematicSpawnOverrides spawnOverrides = HordeSchematicSpawnOverridesFactory.from(type.schematic);
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
            Optional<Location> chestLocation = schematics.resolveChestAnchor(location, type.schematicId());
            Location gateLocation = chestLocation.orElse(location);
            WorldPlacementResult gateResult = gate.checkLocation(gateLocation, regionIndex);
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
        int y = flatSurfaceFinder.naturalGroundY(world, candidate.x(), candidate.z());
        Block surface = world.getBlockAt(candidate.x(), y, candidate.z());
        if (surface.isLiquid() || !surface.isSolid() || isVegetationBlock(surface.getType())) {
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

    private String flatSurfaceIssue(
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
            surfaceHeights[index] = flatSurfaceFinder.naturalGroundY(
                    world,
                    blockX + offset.dx(),
                    blockZ + offset.dz()
            );
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
            Block surface = world.getBlockAt(x, y, z);
            if (surface.isLiquid() || !surface.isSolid() || isVegetationBlock(surface.getType())) {
                return "flat-bad-surface block=" + surface.getType().name() + " at=" + x + "," + y + "," + z;
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
                if (!above.isPassable() && !above.isEmpty() && !isClearableObstruction(above.getType())) {
                    return "flat-no-air block=" + above.getType().name()
                            + " at=" + x + "," + (y + offsetY) + "," + z;
                }
            }
        }
        return null;
    }

    private static boolean isVegetationBlock(Material type) {
        String name = type.name();
        return name.endsWith("_LOG")
                || name.endsWith("_LEAVES")
                || name.endsWith("_SAPLING")
                || type == Material.VINE
                || type == Material.MANGROVE_ROOTS
                || type == Material.BAMBOO
                || type == Material.TALL_GRASS
                || type == Material.SHORT_GRASS
                || type == Material.FERN
                || type == Material.LARGE_FERN
                || type == Material.DEAD_BUSH;
    }

    private static boolean isClearableObstruction(Material type) {
        return isVegetationBlock(type)
                || type == Material.SNOW
                || type.name().endsWith("_FLOWER")
                || type == Material.DANDELION
                || type == Material.POPPY
                || type == Material.BLUE_ORCHID
                || type == Material.ALLIUM
                || type == Material.AZURE_BLUET
                || type == Material.RED_TULIP
                || type == Material.ORANGE_TULIP
                || type == Material.WHITE_TULIP
                || type == Material.PINK_TULIP
                || type == Material.OXEYE_DAISY
                || type == Material.CORNFLOWER
                || type == Material.LILY_OF_THE_VALLEY
                || type == Material.SUNFLOWER
                || type == Material.LILAC
                || type == Material.ROSE_BUSH
                || type == Material.PEONY;
    }

    public static List<FlatSurfaceOffset> footprintFor(HordeTypeSettings type, SchematicService schematics) {
        if (type.usesSchematic()) {
            return schematics.footprint(type.schematicId());
        }
        if (type.builtinNexus.enabled) {
            return HordeBuiltinNexusBuilder.footprintOffsets(type.builtinNexus);
        }
        return List.of(new FlatSurfaceOffset(0, 0));
    }

    public List<FlatSurfaceOffset> footprintFor(HordeTypeSettings type) {
        return footprintFor(type, schematics);
    }

    public void findAsync(
            Plugin plugin,
            HordeTypeSettings type,
            World world,
            WorldPlacementGate gate,
            Consumer<Optional<Location>> callback
    ) {
        findAsync(plugin, type, world, gate, null, callback);
    }

    public void findAsync(
            Plugin plugin,
            HordeTypeSettings type,
            World world,
            WorldPlacementGate gate,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        List<Candidate> candidates = generateCandidates(type, world);
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(type.randomSpawn, world);
        SearchProfile profile = new SearchProfile(
                type.usesSchematic(),
                type.schematicId(),
                type.randomSpawn.maxAttempts,
                type.randomSpawn.requireFlatSurface,
                type.randomSpawn.skipWaterBiomes,
                type.schematic.placement.maxSurfaceDelta,
                type.randomSpawn.searchTimeoutSeconds,
                type.randomSpawn.parallelAttempts,
                type.randomSpawn.loadedChunkCandidateLimit
        );
        if (debug != null) {
            debug.start(world.getName(), area, profile);
        }
        if (candidates.isEmpty()) {
            if (debug != null) {
                if (area.maxRadius() <= 0 && area.hasBoundary()) {
                    debug.noCandidates("no valid spawn radius (maxRadius=0 with map boundary)");
                } else {
                    debug.noCandidates("no candidates generated (check min/max radius and map boundary)");
                }
            }
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(Optional.empty()));
            return;
        }
        WorldGuardRegionIndex regionIndex = WorldGuardIntegrations.regionIndex(world);
        HordeRandomSpawnSettings spawn = type.randomSpawn;
        long deadlineMillis = System.currentTimeMillis() + Math.max(1, spawn.searchTimeoutSeconds) * 1000L;
        AtomicBoolean finished = new AtomicBoolean(false);
        for (int attemptIndex = 0; attemptIndex < candidates.size(); attemptIndex++) {
            if (finished.get() || System.currentTimeMillis() >= deadlineMillis) {
                break;
            }
            Candidate candidate = candidates.get(attemptIndex);
            if (!world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
                continue;
            }
            if (tryCandidate(
                    world,
                    type,
                    gate,
                    regionIndex,
                    candidate,
                    attemptIndex,
                    candidates.size(),
                    finished,
                    debug,
                    callback
            )) {
                return;
            }
        }
        if (finished.get()) {
            return;
        }
        findAsyncBatch(
                plugin,
                type,
                world,
                gate,
                regionIndex,
                candidates,
                0,
                deadlineMillis,
                finished,
                debug,
                callback
        );
    }

    private boolean tryCandidate(
            World world,
            HordeTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            Candidate candidate,
            int attemptIndex,
            int totalAttempts,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        CandidateValidation validation = validateCandidateDetailed(
                world,
                candidate,
                type,
                gate,
                regionIndex
        );
        if (validation.location().isPresent()) {
            if (!finished.compareAndSet(false, true)) {
                return true;
            }
            Location location = validation.location().get();
            if (debug != null) {
                debug.success(
                        attemptIndex,
                        totalAttempts,
                        candidate.x(),
                        location.getBlockY(),
                        candidate.z()
                );
            }
            callback.accept(validation.location());
            return true;
        }
        if (debug != null) {
            debug.reject(
                    attemptIndex,
                    totalAttempts,
                    candidate.x(),
                    candidate.z(),
                    validation.rejectionReason()
            );
        }
        return false;
    }

    private void findAsyncBatch(
            Plugin plugin,
            HordeTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            List<Candidate> candidates,
            int startIndex,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        if (finished.get()) {
            return;
        }
        if (startIndex >= candidates.size() || System.currentTimeMillis() >= deadlineMillis) {
            finishSearch(plugin, type, deadlineMillis, finished, debug, callback);
            return;
        }
        while (startIndex < candidates.size()) {
            Candidate skipCandidate = candidates.get(startIndex);
            if (!world.isChunkLoaded(skipCandidate.chunkX(), skipCandidate.chunkZ())) {
                break;
            }
            startIndex++;
        }
        final int batchStart = startIndex;
        if (batchStart >= candidates.size() || System.currentTimeMillis() >= deadlineMillis) {
            finishSearch(plugin, type, deadlineMillis, finished, debug, callback);
            return;
        }
        HordeRandomSpawnSettings spawn = type.randomSpawn;
        int parallel = Math.max(1, spawn.parallelAttempts);
        final int batchEnd = Math.min(batchStart + parallel, candidates.size());
        int batchSize = batchEnd - batchStart;
        @SuppressWarnings("unchecked")
        CompletableFuture<Chunk>[] chunkFutures = new CompletableFuture[batchSize];
        int asyncCount = 0;
        for (int index = 0; index < batchSize; index++) {
            Candidate candidate = candidates.get(batchStart + index);
            if (world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
                chunkFutures[index] = CompletableFuture.completedFuture(
                        world.getChunkAt(candidate.chunkX(), candidate.chunkZ())
                );
                continue;
            }
            asyncCount++;
            chunkFutures[index] = world.getChunkAtAsync(candidate.chunkX(), candidate.chunkZ(), true);
        }
        Runnable batchTask = () -> processBatch(
                plugin,
                type,
                world,
                gate,
                regionIndex,
                candidates,
                batchStart,
                batchEnd,
                chunkFutures,
                deadlineMillis,
                finished,
                debug,
                callback
        );
        if (asyncCount == 0) {
            plugin.getServer().getScheduler().runTask(plugin, batchTask);
            return;
        }
        CompletableFuture.allOf(chunkFutures)
                .whenComplete((ignored, loadError) -> plugin.getServer().getScheduler().runTask(plugin, batchTask));
    }

    private void finishSearch(
            Plugin plugin,
            HordeTypeSettings type,
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

    private void processBatch(
            Plugin plugin,
            HordeTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            List<Candidate> candidates,
            int startIndex,
            int endIndex,
            CompletableFuture<Chunk>[] chunkFutures,
            long deadlineMillis,
            AtomicBoolean finished,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        if (finished.get()) {
            return;
        }
        int batchSize = endIndex - startIndex;
        for (int index = 0; index < batchSize; index++) {
            if (finished.get()) {
                return;
            }
            Candidate candidate = candidates.get(startIndex + index);
            int attemptIndex = startIndex + index;
            Chunk chunk = chunkFutures[index].getNow(null);
            if (chunk == null) {
                if (debug != null) {
                    debug.reject(
                            attemptIndex,
                            candidates.size(),
                            candidate.x(),
                            candidate.z(),
                            "chunk-load-failed"
                    );
                }
                continue;
            }
            if (tryCandidate(
                    world,
                    type,
                    gate,
                    regionIndex,
                    candidate,
                    attemptIndex,
                    candidates.size(),
                    finished,
                    debug,
                    callback
            )) {
                return;
            }
        }
        findAsyncBatch(
                plugin,
                type,
                world,
                gate,
                regionIndex,
                candidates,
                endIndex,
                deadlineMillis,
                finished,
                debug,
                callback
        );
    }

    public Optional<Location> find(HordeTypeSettings type, World world, WorldPlacementGate gate) {
        WorldGuardRegionIndex regionIndex = WorldGuardIntegrations.regionIndex(world);
        for (Candidate candidate : generateCandidates(type, world)) {
            if (!world.isChunkLoaded(candidate.chunkX(), candidate.chunkZ())) {
                world.loadChunk(candidate.chunkX(), candidate.chunkZ());
            }
            Optional<Location> location = validateCandidate(world, candidate, type, gate, regionIndex);
            if (location.isPresent()) {
                return location;
            }
        }
        return Optional.empty();
    }

    public Optional<Location> findInWorlds(HordeTypeSettings type, WorldPlacementGate gate, List<String> worldNames) {
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
}
