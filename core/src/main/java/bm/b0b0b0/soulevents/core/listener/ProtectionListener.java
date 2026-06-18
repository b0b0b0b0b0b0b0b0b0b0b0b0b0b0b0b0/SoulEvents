package bm.b0b0b0.soulevents.core.listener;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import org.bukkit.event.EventHandler;
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

    @EventHandler(ignoreCancelled = true)
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

    @EventHandler(ignoreCancelled = true)
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
        for (ActiveEvent event : api.modules().activeEvents()) {
            if (event.anchor().getWorld().equals(location.getWorld())
                    && event.anchor().distanceSquared(location) <= 64 * 64) {
                return event.sessionId();
            }
        }
        return null;
    }
}
