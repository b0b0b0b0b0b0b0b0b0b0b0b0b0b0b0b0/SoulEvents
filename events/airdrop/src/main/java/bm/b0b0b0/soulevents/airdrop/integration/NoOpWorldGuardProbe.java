package bm.b0b0b0.soulevents.airdrop.integration;

import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import org.bukkit.Location;

import java.util.Collections;
import java.util.Set;

public final class NoOpWorldGuardProbe implements WorldGuardProbe {

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public Set<String> regionsAt(Location location) {
        return Collections.emptySet();
    }
}
