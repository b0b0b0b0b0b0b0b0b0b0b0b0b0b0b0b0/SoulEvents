package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.function.Consumer;

public final class SpawnWorldResolver {

    private final RandomLocationFinder locationFinder;

    public SpawnWorldResolver(FlatSurfaceFinder flatSurfaceFinder, SchematicService schematics) {
        this.locationFinder = new RandomLocationFinder(flatSurfaceFinder, schematics);
    }

    public void resolveAsync(
            Plugin plugin,
            AirDropTypeSettings type,
            WorldPlacementGate gate,
            Consumer<Optional<Location>> callback
    ) {
        resolveAsync(plugin, type, gate, null, callback);
    }

    public void resolveAsync(
            Plugin plugin,
            AirDropTypeSettings type,
            WorldPlacementGate gate,
            SpawnSearchDebug debug,
            Consumer<Optional<Location>> callback
    ) {
        String spawnWorldName = type.worldPlacement.spawnWorld;
        if (spawnWorldName == null || spawnWorldName.isEmpty()) {
            callback.accept(locationFinder.findInWorlds(type, gate, gate.schedulerWorlds()));
            return;
        }
        World world = Bukkit.getWorld(spawnWorldName);
        if (world == null) {
            if (debug != null) {
                debug.finishFailedWorld(spawnWorldName, "world not loaded");
            }
            callback.accept(Optional.empty());
            return;
        }
        WorldPlacementResult worldCheck = gate.checkWorld(world);
        if (!worldCheck.allowed()) {
            if (debug != null) {
                debug.finishFailedWorld(
                        spawnWorldName,
                        SpawnSearchDebug.gateReason(worldCheck.denial().name(), worldCheck.regionName())
                );
            }
            callback.accept(Optional.empty());
            return;
        }
        locationFinder.findAsync(plugin, type, world, gate, debug, callback);
    }

    public Optional<Location> resolve(AirDropTypeSettings type, WorldPlacementGate gate) {
        String spawnWorldName = type.worldPlacement.spawnWorld;
        if (spawnWorldName == null || spawnWorldName.isEmpty()) {
            return locationFinder.findInWorlds(type, gate, gate.schedulerWorlds());
        }
        World world = Bukkit.getWorld(spawnWorldName);
        if (world == null) {
            return Optional.empty();
        }
        if (!gate.checkWorld(world).allowed()) {
            return Optional.empty();
        }
        return locationFinder.find(type, world, gate);
    }

    public String configuredWorldName(AirDropTypeSettings type) {
        return type.worldPlacement.spawnWorld;
    }
}
