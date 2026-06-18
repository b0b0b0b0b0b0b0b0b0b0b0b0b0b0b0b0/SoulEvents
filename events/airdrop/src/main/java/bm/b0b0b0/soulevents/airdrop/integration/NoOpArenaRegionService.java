package bm.b0b0b0.soulevents.airdrop.integration;

import bm.b0b0b0.soulevents.airdrop.config.settings.ArenaWorldGuardSettings;
import org.bukkit.Location;

import java.util.UUID;

public final class NoOpArenaRegionService implements ArenaRegionService {

    @Override
    public void create(UUID sessionId, Location center, int horizontalRadius, ArenaWorldGuardSettings settings) {
    }

    @Override
    public void remove(UUID sessionId) {
    }

    @Override
    public void shutdown() {
    }
}
