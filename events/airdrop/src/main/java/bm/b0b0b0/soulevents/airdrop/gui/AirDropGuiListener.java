package bm.b0b0b0.soulevents.airdrop.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class AirDropGuiListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropAdminMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropTypeSettingsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropRequirementsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropLootHubMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropLootPoolMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropCreateMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropRequiredItemsMenu menu) {
            menu.handleDrag(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleDrag(event);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropLootPoolMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (event.getView().getTopInventory().getHolder(false) instanceof AirDropLootPoolMenu menu) {
            menu.handleClose(player);
        }
    }
}
