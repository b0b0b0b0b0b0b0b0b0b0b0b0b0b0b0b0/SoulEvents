package bm.b0b0b0.soulevents.core.listener;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

import java.util.Map;
import java.util.UUID;

public final class ProtectionListener implements Listener {

    private final SoulEventsApi api;

    public ProtectionListener(SoulEventsApi api) {
        this.api = api;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event) {
        UUID sessionId = findSession(event.getBlock().getLocation());
        if (sessionId == null) {
            return;
        }
        if (!api.protection().arena().canModifyBlock(sessionId, event.getBlock(), event.getPlayer())) {
            event.setCancelled(true);
            api.messages().send(event.getPlayer(), "protection.arena.no-build", Map.of());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        UUID sessionId = findSession(event.getBlock().getLocation());
        if (sessionId == null) {
            return;
        }
        if (!api.protection().arena().canPlaceFluid(sessionId, event.getBlock().getLocation(), event.getPlayer())) {
            event.setCancelled(true);
            api.messages().send(event.getPlayer(), "protection.arena.no-fluid", Map.of());
        }
    }

    private UUID findSession(org.bukkit.Location location) {
        if (location.getWorld() == null) {
            return null;
        }
        for (ActiveEvent event : api.modules().activeEvents()) {
            org.bukkit.Location anchor = event.anchor();
            if (anchor.getWorld() != location.getWorld()) {
                continue;
            }
            if (anchor.distanceSquared(location) <= 64 * 64) {
                return event.sessionId();
            }
        }
        return null;
    }
}
