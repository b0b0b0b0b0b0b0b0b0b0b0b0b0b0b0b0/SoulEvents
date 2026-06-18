package bm.b0b0b0.soulevents.airdrop.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class AirDropGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder(false) instanceof AirDropAdminMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof AirDropTypeSettingsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getInventory().getHolder(false) instanceof AirDropCreateMenu menu) {
            menu.handleClick(event);
        }
    }
}
