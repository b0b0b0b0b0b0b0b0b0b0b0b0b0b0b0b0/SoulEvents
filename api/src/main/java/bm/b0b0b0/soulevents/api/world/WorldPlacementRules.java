package bm.b0b0b0.soulevents.api.world;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class WorldPlacementRules {

    private final WorldListMode worldListMode;
    private final List<String> worlds;
    private final boolean worldGuardEnabled;
    private final WorldListMode regionListMode;
    private final List<String> regions;

    public WorldPlacementRules(
            WorldListMode worldListMode,
            List<String> worlds,
            boolean worldGuardEnabled,
            WorldListMode regionListMode,
            List<String> regions
    ) {
        this.worldListMode = worldListMode;
        this.worlds = List.copyOf(worlds);
        this.worldGuardEnabled = worldGuardEnabled;
        this.regionListMode = regionListMode;
        this.regions = List.copyOf(regions);
    }

    public List<String> configuredWorlds() {
        return worlds;
    }

    public boolean allowsWorld(World world) {
        return checkWorld(world != null ? world.getName() : null).allowed();
    }

    public boolean allowsWorldName(String worldName) {
        return checkWorld(worldName).allowed();
    }

    public boolean allowsLocation(Location location, WorldGuardProbe probe) {
        return checkLocation(location, probe).allowed();
    }

    public WorldPlacementResult checkLocation(Location location, WorldGuardProbe probe) {
        if (location == null || location.getWorld() == null) {
            return WorldPlacementResult.deny(WorldPlacementDenial.INVALID_LOCATION, "", "");
        }
        WorldPlacementResult worldResult = checkWorld(location.getWorld().getName());
        if (!worldResult.allowed()) {
            return worldResult;
        }
        if (!worldGuardEnabled) {
            return WorldPlacementResult.allow();
        }
        if (probe == null || !probe.available()) {
            return WorldPlacementResult.deny(
                    WorldPlacementDenial.WORLD_GUARD_MISSING,
                    location.getWorld().getName(),
                    ""
            );
        }
        return checkRegions(location, probe);
    }

    public WorldPlacementResult checkWorld(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return WorldPlacementResult.deny(WorldPlacementDenial.INVALID_LOCATION, "", "");
        }
        boolean listed = worlds.stream().anyMatch(entry -> entry.equalsIgnoreCase(worldName));
        if (worldListMode == WorldListMode.WHITELIST) {
            if (!worlds.isEmpty() && !listed) {
                return WorldPlacementResult.deny(WorldPlacementDenial.WORLD_NOT_WHITELISTED, worldName, "");
            }
            return WorldPlacementResult.allow();
        }
        if (listed) {
            return WorldPlacementResult.deny(WorldPlacementDenial.WORLD_BLACKLISTED, worldName, "");
        }
        return WorldPlacementResult.allow();
    }

    private WorldPlacementResult checkRegions(Location location, WorldGuardProbe probe) {
        String worldName = location.getWorld().getName();
        Set<String> present = probe.regionsAt(location);
        if (regions.isEmpty()) {
            if (regionListMode == WorldListMode.WHITELIST) {
                return WorldPlacementResult.deny(WorldPlacementDenial.REGION_NOT_WHITELISTED, worldName, "");
            }
            return WorldPlacementResult.allow();
        }
        String matchedRegion = findMatchedRegion(present);
        boolean matches = matchedRegion != null;
        if (regionListMode == WorldListMode.WHITELIST) {
            if (!matches) {
                return WorldPlacementResult.deny(WorldPlacementDenial.REGION_NOT_WHITELISTED, worldName, "");
            }
            return WorldPlacementResult.allow();
        }
        if (matches) {
            return WorldPlacementResult.deny(WorldPlacementDenial.REGION_BLACKLISTED, worldName, matchedRegion);
        }
        return WorldPlacementResult.allow();
    }

    private String findMatchedRegion(Set<String> present) {
        for (String configured : regions) {
            for (String found : present) {
                if (found.equalsIgnoreCase(configured)) {
                    return found;
                }
            }
        }
        return null;
    }

    public List<String> normalizeWorldNames() {
        return worlds.stream().map(name -> name.toLowerCase(Locale.ROOT)).toList();
    }
}
