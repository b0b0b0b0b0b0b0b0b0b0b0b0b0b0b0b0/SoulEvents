package bm.b0b0b0.soulevents.volcano.gui;

import bm.b0b0b0.soulevents.volcano.config.VolcanoPermissions;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class VolcanoGuiListener implements Listener {

    private final VolcanoMessageService messages;

    public VolcanoGuiListener(VolcanoMessageService messages) {
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
        if (holder instanceof VolcanoAdminMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof VolcanoTypeSettingsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof VolcanoLootHubMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof VolcanoObfuscationItemsMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof VolcanoLootPoolMenu menu) {
            menu.handleClick(event);
            return;
        }
        if (holder instanceof VolcanoCreateMenu menu) {
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
        if (holder instanceof VolcanoObfuscationItemsMenu menu) {
            menu.handleDrag(event);
            return;
        }
        if (holder instanceof VolcanoLootPoolMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (holder instanceof VolcanoObfuscationItemsMenu menu) {
            menu.handleClose(player);
            return;
        }
        if (holder instanceof VolcanoLootPoolMenu menu) {
            menu.handleClose(player);
        }
    }

    private boolean denyUnlessAdmin(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return true;
        }
        if (player.hasPermission(VolcanoPermissions.STAFF)) {
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
        if (player.hasPermission(VolcanoPermissions.STAFF)) {
            return false;
        }
        event.setCancelled(true);
        messages.send(player, "command.no-permission", Map.of());
        return true;
    }

    private static boolean isAdminGui(InventoryHolder holder) {
        return holder instanceof VolcanoAdminMenu
                || holder instanceof VolcanoTypeSettingsMenu
                || holder instanceof VolcanoLootHubMenu
                || holder instanceof VolcanoObfuscationItemsMenu
                || holder instanceof VolcanoLootPoolMenu
                || holder instanceof VolcanoCreateMenu;
    }

    private static boolean isEditableAdminGui(InventoryHolder holder) {
        return holder instanceof VolcanoObfuscationItemsMenu
                || holder instanceof VolcanoLootPoolMenu;
    }
}
