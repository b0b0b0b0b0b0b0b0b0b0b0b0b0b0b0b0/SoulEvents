package bm.b0b0b0.soulevents.airdrop.integration;

import bm.b0b0b0.soulevents.airdrop.config.settings.ArenaWorldGuardSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public interface ArenaRegionService {

    void create(
            UUID sessionId,
            Location anchor,
            ArenaWorldGuardSettings settings,
            Optional<SchematicWorldBounds> schematicBounds
    );

    void remove(UUID sessionId);

    void shutdown();

    Optional<UUID> sessionAt(Location location);
}
