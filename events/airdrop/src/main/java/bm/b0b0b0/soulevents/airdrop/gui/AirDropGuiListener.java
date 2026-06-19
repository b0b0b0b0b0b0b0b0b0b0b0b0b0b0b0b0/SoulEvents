package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.config.AirDropPermissions;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class AirDropGuiListener implements Listener {

    private final AirDropMessageService messages;

    public AirDropGuiListener(AirDropMessageService messages) {
        this.messages = messages;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!isAdminGui(holder)) {
            return;
        }
        if (denyUnlessAdmin(event)) {
            return;
        }
        if (holder instanceof AirDropAdminMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropTypeSettingsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropRequirementsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropLootHubMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof AirDropCreateMenu menu) {
            menu.handleClick(event);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (!isEditableAdminGui(holder)) {
            return;
        }
        if (denyUnlessAdmin(event)) {
            return;
        }
        if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleDrag(event);
            return;
        }
        if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleDrag(event);
            return;
        }
        if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (holder instanceof AirDropRequiredItemsMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (holder instanceof AirDropObfuscationItemsMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (holder instanceof AirDropLootPoolMenu menu) {
            menu.handleClose(player);
        }
    }

    private boolean denyUnlessAdmin(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return true;
        }
        if (player.hasPermission(AirDropPermissions.STAFF)) {
            return false;
        }
        event.setCancelled(true);
        messages.send(player, "command.no-permission", Map.of());
        return true;
    }

    private boolean denyUnlessAdmin(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return true;
        }
        if (player.hasPermission(AirDropPermissions.STAFF)) {
            return false;
        }
        event.setCancelled(true);
        messages.send(player, "command.no-permission", Map.of());
        return true;
    }

    private static boolean isAdminGui(InventoryHolder holder) {
        return holder instanceof AirDropAdminMenu
                || holder instanceof AirDropTypeSettingsMenu
                || holder instanceof AirDropRequirementsMenu
                || holder instanceof AirDropRequiredItemsMenu
                || holder instanceof AirDropLootHubMenu
                || holder instanceof AirDropObfuscationItemsMenu
                || holder instanceof AirDropLootPoolMenu
                || holder instanceof AirDropCreateMenu;
    }

    private static boolean isEditableAdminGui(InventoryHolder holder) {
        return holder instanceof AirDropRequiredItemsMenu
                || holder instanceof AirDropObfuscationItemsMenu
                || holder instanceof AirDropLootPoolMenu;
    }
}
