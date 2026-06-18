package bm.b0b0b0.soulevents.airdrop.integration;

import bm.b0b0b0.soulevents.airdrop.config.settings.ArenaWorldGuardSettings;
import org.bukkit.Location;

import java.util.UUID;

public interface ArenaRegionService {

    void create(UUID sessionId, Location center, int horizontalRadius, ArenaWorldGuardSettings settings);

    void remove(UUID sessionId);

    void shutdown();
}
