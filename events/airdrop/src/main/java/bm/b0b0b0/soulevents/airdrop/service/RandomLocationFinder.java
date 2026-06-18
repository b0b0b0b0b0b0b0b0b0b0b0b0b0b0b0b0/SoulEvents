package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.RandomSpawnSettings;
import bm.b0b0b0.soulevents.airdrop.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardRegionIndex;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceRequirements;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class RandomLocationFinder {

    private final FlatSurfaceFinder flatSurfaceFinder;

    public RandomLocationFinder(FlatSurfaceFinder flatSurfaceFinder) {
        this.flatSurfaceFinder = flatSurfaceFinder;
    }

    public record Candidate(int x, int z, int chunkX, int chunkZ) {
    }

    public static List<Candidate> generateCandidates(AirDropTypeSettings type, World world) {
        RandomSpawnSettings spawn = type.randomSpawn;
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(spawn, world);
        if (area.maxRadius() <= 0 && area.hasBoundary()) {
            return List.of();
        }
        Random random = ThreadLocalRandom.current();
        List<Candidate> candidates = new ArrayList<>(spawn.maxAttempts);
        for (int attempt = 0; attempt < spawn.maxAttempts; attempt++) {
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
            AirDropTypeSettings type,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex
    ) {
        MapSpawnBoundary.Area area = MapSpawnBoundary.resolve(type.randomSpawn, world);
        if (!area.containsBlock(candidate.x(), candidate.z())) {
            return Optional.empty();
        }
        RandomSpawnSettings spawn = type.randomSpawn;
        Optional<Location> surfaceLocation;
        if (spawn.requireFlatSurface) {
            FlatSurfaceRequirements requirements = new FlatSurfaceRequirements(
                    spawn.flatMaxHeightDelta,
                    spawn.surfaceYOffset,
                    spawn.flatMinAirAbove,
                    true
            );
            surfaceLocation = flatSurfaceFinder.resolve(
                    world,
                    candidate.x(),
                    candidate.z(),
                    requirements,
                    footprintFor(type)
            );
        } else {
            int y = world.getHighestBlockYAt(candidate.x(), candidate.z());
            Block surface = world.getBlockAt(candidate.x(), y, candidate.z());
            if (surface.isLiquid() || !surface.isSolid()) {
                return Optional.empty();
            }
            surfaceLocation = Optional.of(new Location(
                    world,
                    candidate.x() + 0.5,
                    y + spawn.surfaceYOffset,
                    candidate.z() + 0.5
            ));
        }
        if (surfaceLocation.isEmpty()) {
            return Optional.empty();
        }
        Location location = surfaceLocation.get();
        if (gate.checkLocation(location, regionIndex).allowed()) {
            return Optional.of(location);
        }
        return Optional.empty();
    }

    public static List<FlatSurfaceOffset> footprintFor(AirDropTypeSettings type) {
        if (type.chestCluster.enabled) {
            return AirDropClusterChestPlacer.footprintOffsets();
        }
        return List.of(new FlatSurfaceOffset(0, 0));
    }

    public void findAsync(
            Plugin plugin,
            AirDropTypeSettings type,
            World world,
            WorldPlacementGate gate,
            Consumer<Optional<Location>> callback
    ) {
        List<Candidate> candidates = generateCandidates(type, world);
        WorldGuardRegionIndex regionIndex = WorldGuardIntegrations.regionIndex(world);
        findAsyncAttempt(plugin, type, world, gate, regionIndex, candidates, 0, callback);
    }

    private void findAsyncAttempt(
            Plugin plugin,
            AirDropTypeSettings type,
            World world,
            WorldPlacementGate gate,
            WorldGuardRegionIndex regionIndex,
            List<Candidate> candidates,
            int attempt,
            Consumer<Optional<Location>> callback
    ) {
        if (attempt >= candidates.size()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> callback.accept(Optional.empty()));
            return;
        }
        Candidate candidate = candidates.get(attempt);
        world.getChunkAtAsync(candidate.chunkX(), candidate.chunkZ(), true).whenComplete((chunk, error) ->
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Optional<Location> location = validateCandidate(world, candidate, type, gate, regionIndex);
                    if (location.isPresent()) {
                        callback.accept(location);
                        return;
                    }
                    findAsyncAttempt(plugin, type, world, gate, regionIndex, candidates, attempt + 1, callback);
                })
        );
    }

    public Optional<Location> find(AirDropTypeSettings type, World world, WorldPlacementGate gate) {
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

    public Optional<Location> findInWorlds(AirDropTypeSettings type, WorldPlacementGate gate, List<String> worldNames) {
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
