package bm.b0b0b0.soulevents.airdrop.gui;

import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

public final class AirDropGuiListener implements Listener {

    private static final String ADMIN_PERMISSION = "soulevents.airdrop.admin";

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!isAirDropMenu(holder)) {
            return;
        }
        if (!hasAdminPermission(event.getWhoClicked())) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            return;
        }
        if (holder instanceof AirDropAdminMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropTypeSettingsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropRequirementsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropLootHubMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof AirDropCreateMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!isAirDropMenu(holder)) {
            return;
        }
        if (!hasAdminPermission(event.getWhoClicked())) {
            event.setCancelled(true);
            event.getWhoClicked().closeInventory();
            return;
        }
        if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleDrag(event);
        } else if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleDrag(event);
        } else if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!isAirDropMenu(holder)) {
            return;
        }
        if (!player.hasPermission(ADMIN_PERMISSION)) {
            return;
        }
        if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClose(player);
        } else if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClose(player);
        } else if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleClose(player);
        }
    }

    private static boolean hasAdminPermission(HumanEntity who) {
        return who.hasPermission(ADMIN_PERMISSION);
    }

    private static boolean isAirDropMenu(InventoryHolder holder) {
        return holder instanceof AirDropAdminMenu
                || holder instanceof AirDropTypeSettingsMenu
                || holder instanceof AirDropRequirementsMenu
                || holder instanceof AirDropRequiredItemsMenu
                || holder instanceof AirDropLootHubMenu
                || holder instanceof AirDropObfuscationItemsMenu
                || holder instanceof AirDropLootPoolMenu
                || holder instanceof AirDropCreateMenu;
    }
}
