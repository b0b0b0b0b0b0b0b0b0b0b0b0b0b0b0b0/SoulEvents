package bm.b0b0b0.soulevents.volcano.listener;

import bm.b0b0b0.soulevents.volcano.service.VolcanoService;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public final class VolcanoItemPickupListener implements Listener {

    private final VolcanoService service;

    public VolcanoItemPickupListener(VolcanoService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getItem() instanceof Item item)) {
            return;
        }
        if (service.sessions().sessionIdForEntity(item.getUniqueId()).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        service.handleItemPickup(player, item);
    }
}
