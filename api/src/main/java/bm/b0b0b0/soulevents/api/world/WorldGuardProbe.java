package bm.b0b0b0.soulevents.api.world;

import org.bukkit.Location;

import java.util.Collections;
import java.util.Set;

public interface WorldGuardProbe {

    boolean available();

    Set<String> regionsAt(Location location);

    default boolean isInsideAnyRegion(Location location, Set<String> ignoredRegionIds) {
        return false;
    }

    default int nearestRegionDistanceBlocks(Location location, Set<String> ignoredRegionIds) {
        return Integer.MAX_VALUE;
    }

    default Set<String> ignoredRegionIdPrefixes() {
        return Collections.emptySet();
    }
}
