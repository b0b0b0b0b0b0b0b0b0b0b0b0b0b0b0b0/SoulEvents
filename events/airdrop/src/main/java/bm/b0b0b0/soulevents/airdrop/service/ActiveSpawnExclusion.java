package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.airdrop.config.settings.RandomSpawnSettings;
import bm.b0b0b0.soulevents.airdrop.module.AirDropModule;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Optional;

public final class ActiveSpawnExclusion {

    private final SoulEventsApi api;

    public ActiveSpawnExclusion(SoulEventsApi api) {
        this.api = api;
    }

    public Optional<String> conflict(World world, Location anchor, RandomSpawnSettings spawn) {
        int minDistance = Math.max(0, spawn.minBlocksFromActiveSession);
        if (minDistance <= 0 || anchor.getWorld() == null) {
            return Optional.empty();
        }
        for (ActiveEvent event : api.modules().activeEvents(AirDropModule.MODULE_ID)) {
            Location activeAnchor = event.anchor();
            if (activeAnchor.getWorld() == null || !world.equals(activeAnchor.getWorld())) {
                continue;
            }
            double distance = distanceBlocks(anchor, activeAnchor);
            if (distance < minDistance) {
                return Optional.of(
                        "too-close-to-active type=" + event.typeId()
                                + " at=" + activeAnchor.getBlockX() + ","
                                + activeAnchor.getBlockY() + ","
                                + activeAnchor.getBlockZ()
                                + " distance=" + (int) Math.floor(distance)
                                + " min=" + minDistance
                );
            }
        }
        return Optional.empty();
    }

    private static double distanceBlocks(Location first, Location second) {
        double dx = first.getBlockX() - second.getBlockX();
        double dy = first.getBlockY() - second.getBlockY();
        double dz = first.getBlockZ() - second.getBlockZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
