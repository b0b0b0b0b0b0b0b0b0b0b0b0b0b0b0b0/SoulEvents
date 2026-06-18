package bm.b0b0b0.soulevents.airdrop.gate;

import bm.b0b0b0.soulevents.airdrop.config.settings.WorldPlacementSettings;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardRegionIndex;
import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import bm.b0b0b0.soulevents.api.world.WorldPlacementDenial;
import bm.b0b0b0.soulevents.api.world.WorldPlacementResult;
import bm.b0b0b0.soulevents.api.world.WorldPlacementRules;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class WorldPlacementGate {

    private final WorldPlacementSettings settings;
    private final WorldPlacementRules rules;
    private final WorldGuardProbe worldGuardProbe;

    public WorldPlacementGate(WorldPlacementSettings settings, WorldGuardProbe worldGuardProbe) {
        this.settings = settings;
        this.rules = new WorldPlacementRules(
                settings.worldListMode,
                settings.worlds,
                settings.worldGuardEnabled,
                settings.regionListMode,
                settings.regions
        );
        this.worldGuardProbe = worldGuardProbe;
    }

    public WorldPlacementResult checkWorld(World world) {
        if (world == null) {
            return rules.checkWorld(null);
        }
        return rules.checkWorld(world.getName());
    }

    public WorldPlacementResult checkLocation(Location location) {
        return checkLocation(location, null);
    }

    public WorldPlacementResult checkLocation(Location location, WorldGuardRegionIndex regionIndex) {
        WorldPlacementResult base = rules.checkLocation(location, worldGuardProbe);
        if (!base.allowed()) {
            return base;
        }
        return checkRegionProximity(location, regionIndex);
    }

    public boolean allowsLocation(Location location) {
        return checkLocation(location).allowed();
    }

    public List<String> schedulerWorlds() {
        List<String> result = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            if (rules.allowsWorld(world)) {
                result.add(world.getName());
            }
        }
        return List.copyOf(result);
    }

    public World pickScheduledWorld() {
        List<String> allowed = schedulerWorlds();
        if (allowed.isEmpty()) {
            return null;
        }
        for (String worldName : allowed) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                continue;
            }
            Location spawn = world.getSpawnLocation();
            if (allowsLocation(spawn)) {
                return world;
            }
        }
        return null;
    }

    private WorldPlacementResult checkRegionProximity(Location location, WorldGuardRegionIndex regionIndex) {
        if (!requiresRegionProximityCheck()) {
            return WorldPlacementResult.allow();
        }
        if (regionIndex != null) {
            int x = location.getBlockX();
            int y = location.getBlockY();
            int z = location.getBlockZ();
            if (settings.denySpawnInsideRegions && regionIndex.isInsideAny(x, y, z)) {
                return WorldPlacementResult.deny(
                        WorldPlacementDenial.REGION_INSIDE,
                        location.getWorld().getName(),
                        ""
                );
            }
            int minDistance = settings.minBlocksFromNearestRegion;
            if (minDistance > 0) {
                int nearest = regionIndex.nearestDistanceBlocks(x, y, z);
                if (nearest < minDistance) {
                    return WorldPlacementResult.deny(
                            WorldPlacementDenial.REGION_TOO_CLOSE,
                            location.getWorld().getName(),
                            Integer.toString(nearest)
                    );
                }
            }
            return WorldPlacementResult.allow();
        }
        if (worldGuardProbe == null || !worldGuardProbe.available()) {
            return WorldPlacementResult.deny(
                    WorldPlacementDenial.WORLD_GUARD_MISSING,
                    location.getWorld().getName(),
                    ""
            );
        }
        Set<String> ignored = Set.of();
        if (settings.denySpawnInsideRegions && worldGuardProbe.isInsideAnyRegion(location, ignored)) {
            return WorldPlacementResult.deny(
                    WorldPlacementDenial.REGION_INSIDE,
                    location.getWorld().getName(),
                    ""
            );
        }
        int minDistance = settings.minBlocksFromNearestRegion;
        if (minDistance > 0) {
            int nearest = worldGuardProbe.nearestRegionDistanceBlocks(location, ignored);
            if (nearest < minDistance) {
                return WorldPlacementResult.deny(
                        WorldPlacementDenial.REGION_TOO_CLOSE,
                        location.getWorld().getName(),
                        Integer.toString(nearest)
                );
            }
        }
        return WorldPlacementResult.allow();
    }

    private boolean requiresRegionProximityCheck() {
        return settings.denySpawnInsideRegions || settings.minBlocksFromNearestRegion > 0;
    }
}
