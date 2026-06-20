package bm.b0b0b0.soulevents.mobwaves.integration;

import bm.b0b0b0.soulevents.mobwaves.config.settings.ArenaWorldGuardSettings;
import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import org.bukkit.Location;

import java.util.Optional;
import java.util.UUID;

public final class NoOpArenaRegionService implements ArenaRegionService {

    @Override
    public void create(
            UUID sessionId,
            Location anchor,
            ArenaWorldGuardSettings settings,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
    }

    @Override
    public void remove(UUID sessionId) {
    }

    @Override
    public void shutdown() {
    }

    @Override
    public Optional<UUID> sessionAt(Location location) {
        return Optional.empty();
    }
}

