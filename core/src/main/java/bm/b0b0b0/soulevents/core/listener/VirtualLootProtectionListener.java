package bm.b0b0b0.soulevents.core.listener;

import bm.b0b0b0.soulevents.api.inventory.ProtectedLootInventoryHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.EnumSet;
import java.util.Set;

public final class VirtualLootProtectionListener implements Listener {

    private static final Set<InventoryAction> BOTTOM_EXPLOIT_ACTIONS = EnumSet.of(
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.UNKNOWN
    );

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryMove(InventoryMoveItemEvent event) {
        if (isProtectedInventory(event.getSource()) || isProtectedInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isProtectedInventory(top)) {
            return;
        }
        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryCreative(InventoryCreativeEvent event) {
        if (isProtectedInventory(event.getInventory())
                || isProtectedInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isProtectedInventory(top)) {
            return;
        }
        int topSize = top.getSize();
        if (event.getRawSlot() < topSize) {
            return;
        }
        if (BOTTOM_EXPLOIT_ACTIONS.contains(event.getAction())) {
            event.setCancelled(true);
        }
    }

    private static boolean isProtectedInventory(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder(false);
        return holder instanceof ProtectedLootInventoryHolder;
    }
}
