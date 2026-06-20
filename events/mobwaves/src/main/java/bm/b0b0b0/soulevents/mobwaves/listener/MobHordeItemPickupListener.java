package bm.b0b0b0.soulevents.mobwaves.listener;

import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

public final class MobHordeItemPickupListener implements Listener {

    private final MobHordeService service;

    public MobHordeItemPickupListener(MobHordeService service) {
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
