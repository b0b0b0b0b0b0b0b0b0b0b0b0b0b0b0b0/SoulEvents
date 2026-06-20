package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPermissions;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public final class MobWavesGuiListener implements Listener {

    private final MobWaveMessageService messages;

    public MobWavesGuiListener(MobWaveMessageService messages) {
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
        if (holder instanceof MobWavesAdminMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveProfilesMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobHordeTypeSettingsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobHordeLootHubMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobHordeLootPoolMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobHordeObfuscationItemsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveProfileMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveEditorMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveMobSettingsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveMobOverrideMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveWaveSettingsMenu menu) {
            menu.handleClick(event);
        } else if (holder instanceof MobWaveProfileSettingsMenu menu) {
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
        if (holder instanceof MobWaveEditorMenu menu) {
            menu.handleDrag(event);
        } else if (holder instanceof MobHordeLootPoolMenu menu) {
            menu.handleDrag(event);
        } else if (holder instanceof MobHordeObfuscationItemsMenu menu) {
            menu.handleDrag(event);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        InventoryHolder holder = event.getView().getTopInventory().getHolder(false);
        if (holder instanceof MobWaveEditorMenu menu) {
            menu.handleClose(event);
        } else if (holder instanceof MobHordeLootPoolMenu menu) {
            menu.handleClose(player);
        } else if (holder instanceof MobHordeObfuscationItemsMenu menu) {
            menu.handleClose(player);
        }
    }

    private boolean denyUnlessAdmin(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return true;
        }
        if (player.hasPermission(MobWavesPermissions.STAFF)) {
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
        if (player.hasPermission(MobWavesPermissions.STAFF)) {
            return false;
        }
        event.setCancelled(true);
        messages.send(player, "command.no-permission", Map.of());
        return true;
    }

    private static boolean isAdminGui(InventoryHolder holder) {
        return holder instanceof MobWavesAdminMenu
                || holder instanceof MobWaveProfilesMenu
                || holder instanceof MobHordeTypeSettingsMenu
                || holder instanceof MobHordeLootHubMenu
                || holder instanceof MobHordeLootPoolMenu
                || holder instanceof MobHordeObfuscationItemsMenu
                || holder instanceof MobWaveProfileMenu
                || holder instanceof MobWaveEditorMenu
                || holder instanceof MobWaveMobSettingsMenu
                || holder instanceof MobWaveMobOverrideMenu
                || holder instanceof MobWaveWaveSettingsMenu
                || holder instanceof MobWaveProfileSettingsMenu;
    }

    private static boolean isEditableAdminGui(InventoryHolder holder) {
        return holder instanceof MobWaveEditorMenu
                || holder instanceof MobHordeLootPoolMenu
                || holder instanceof MobHordeObfuscationItemsMenu;
    }
}
