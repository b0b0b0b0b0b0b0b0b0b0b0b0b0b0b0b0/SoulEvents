package bm.b0b0b0.soulevents.airdrop.gate;

import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.settings.ArenaWorldGuardSettings;
import bm.b0b0b0.soulevents.airdrop.integration.ArenaRegionService;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import org.bukkit.Location;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ArenaRegionGuard {

    private final AirDropService service;
    private final AirDropPluginConfig config;
    private final ArenaRegionService arenaRegions;

    public ArenaRegionGuard(
            AirDropService service,
            AirDropPluginConfig config,
            ArenaRegionService arenaRegions
    ) {
        this.service = service;
        this.config = config;
        this.arenaRegions = arenaRegions;
    }

    public Optional<ArenaWorldGuardSettings> settingsAt(Location location) {
        Optional<UUID> sessionId = arenaRegions.sessionAt(location);
        if (sessionId.isEmpty()) {
            return Optional.empty();
        }
        Optional<ActiveEvent> active = service.activeEvent(sessionId.get());
        if (active.isEmpty()) {
            return Optional.empty();
        }
        return config.type(active.get().typeId())
                .map(definition -> definition.settings().arenaWorldGuard)
                .filter(settings -> settings.createTempRegion && settings.enforceDenyInPlugin);
    }

    public boolean denies(Location location, String flagName) {
        return settingsAt(location)
                .map(settings -> containsFlag(settings.denyFlags, flagName))
                .orElse(false);
    }

    public boolean deniesFluidBuckets(Location location) {
        return settingsAt(location)
                .map(settings -> settings.denyFluidBuckets)
                .orElse(false);
    }

    private static boolean containsFlag(List<String> denyFlags, String flagName) {
        if (denyFlags == null || denyFlags.isEmpty()) {
            return false;
        }
        String normalized = flagName.toLowerCase(Locale.ROOT);
        for (String denyFlag : denyFlags) {
            if (denyFlag != null && denyFlag.trim().equalsIgnoreCase(normalized)) {
                return true;
            }
        }
        return false;
    }
}
