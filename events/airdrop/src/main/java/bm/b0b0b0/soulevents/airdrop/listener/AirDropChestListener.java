package bm.b0b0b0.soulevents.airdrop.listener;

import bm.b0b0b0.soulevents.airdrop.chest.AirDropChestHolder;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
import bm.b0b0b0.soulevents.api.inventory.ProtectedLootInventoryHolder;
import bm.b0b0b0.soulevents.api.protection.LootGuardService;
import bm.b0b0b0.soulevents.api.protection.ObfuscatedLootRef;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class AirDropChestListener implements Listener {

    private static final Set<InventoryAction> BLOCKED_BOTTOM_ACTIONS = EnumSet.of(
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.UNKNOWN
    );

    private static final Set<ClickType> BLOCKED_BOTTOM_CLICKS = EnumSet.of(
            ClickType.DOUBLE_CLICK,
            ClickType.NUMBER_KEY,
            ClickType.SWAP_OFFHAND,
            ClickType.MIDDLE,
            ClickType.RIGHT,
            ClickType.SHIFT_RIGHT,
            ClickType.SHIFT_LEFT
    );

    private final AirDropService service;

    public AirDropChestListener(AirDropService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Optional<UUID> sessionId = service.sessionIdAt(block.getLocation());
        if (sessionId.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }
        service.tryOpenChest(player, sessionId.get(), block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockChestMove(InventoryMoveItemEvent event) {
        if (isWorldBlockChestMove(event.getSource()) || isWorldBlockChestMove(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLootClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!isAirDropTop(top)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        AirDropChestHolder chest = (AirDropChestHolder) top.getHolder(false);
        int topSize = top.getSize();

        if (event.getRawSlot() < topSize) {
            if (isLegitimateTakeClick(event, top)) {
                handleChestSlotClick(event, player, chest, top);
            }
            return;
        }

        if (blocksBottomInteraction(event, topSize)) {
            return;
        }

        if (event.getClickedInventory() == top) {
            return;
        }

        if (event.isShiftClick() || event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        handleInventoryChange(event.getWhoClicked(), event.getView().getTopInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        handleInventoryChange(event.getWhoClicked(), event.getView().getTopInventory());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (top.getHolder(false) instanceof AirDropChestHolder chest) {
            sanitizeChestInventory(chest, top);
            if (event.getPlayer() instanceof Player player) {
                revealCarriedObfuscated(player);
            }
        }
        handleInventoryChange(event.getPlayer(), top);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent event) {
        if (!service.sessionIdAt(event.getBlock().getLocation()).isPresent()) {
            return;
        }
        event.setCancelled(true);
        service.messages().send(event.getPlayer(), "airdrop.chest-protected", Map.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (service.sessionIdAt(event.getBlock().getLocation()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event) {
        removeAnchorBlocks(event.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event) {
        removeAnchorBlocks(event.blockList().iterator());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (service.sessionIdAt(block.getLocation()).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (service.sessionIdAt(block.getLocation()).isPresent()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    private static boolean isAirDropTop(Inventory top) {
        return top.getHolder(false) instanceof AirDropChestHolder;
    }

    private boolean isWorldBlockChestMove(Inventory inventory) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder(false);
        if (holder instanceof ProtectedLootInventoryHolder) {
            return false;
        }
        if (holder instanceof BlockState state) {
            return service.sessionIdAt(state.getLocation()).isPresent();
        }
        return false;
    }

    private static boolean isLegitimateTakeClick(InventoryClickEvent event, Inventory top) {
        if (event.getClickedInventory() != top) {
            return false;
        }
        if (event.getClick() != ClickType.LEFT) {
            return false;
        }
        if (event.isShiftClick()) {
            return false;
        }
        ItemStack cursor = event.getCursor();
        if (cursor != null && !cursor.getType().isAir()) {
            return false;
        }
        ItemStack current = event.getCurrentItem();
        return current != null && !current.getType().isAir();
    }

    private static boolean blocksBottomInteraction(InventoryClickEvent event, int topSize) {
        if (event.getRawSlot() < topSize) {
            return false;
        }
        return BLOCKED_BOTTOM_ACTIONS.contains(event.getAction())
                || BLOCKED_BOTTOM_CLICKS.contains(event.getClick())
                || event.isShiftClick();
    }

    private void handleChestSlotClick(
            InventoryClickEvent event,
            Player player,
            AirDropChestHolder chest,
            Inventory top
    ) {
        LootGuardService lootGuard = service.lootGuard();
        ItemStack stack = event.getCurrentItem();
        if (stack == null || stack.getType().isAir()) {
            lootGuard.debug("chestClick empty slot=" + event.getSlot() + " player=" + player.getName());
            return;
        }
        Optional<ObfuscatedLootRef> refOptional = lootGuard.obfuscatedRef(stack);
        if (refOptional.isEmpty()) {
            lootGuard.debug("chestClick not obfuscated slot=" + event.getSlot()
                    + " player=" + player.getName()
                    + " item=" + stack.getType().name() + "x" + stack.getAmount());
            return;
        }
        ObfuscatedLootRef ref = refOptional.get();
        if (!ref.sessionId().equals(chest.sessionId())) {
            lootGuard.debugWarn("chestClick session mismatch player=" + player.getName()
                    + " chestSession=" + chest.sessionId()
                    + " itemSession=" + ref.sessionId()
                    + " slot=" + ref.slotIndex());
            return;
        }
        if (!lootGuard.canTake(player, ref.sessionId(), ref.slotIndex())) {
            if (lootGuard.isSlotClaimed(ref.sessionId(), ref.slotIndex())) {
                service.messages().send(player, "airdrop.chest-slot-taken", Map.of());
            } else {
                service.api().messages().send(player, "protection.loot.cooldown", Map.of());
            }
            return;
        }
        lootGuard.debug("chestClick take player=" + player.getName()
                + " session=" + ref.sessionId()
                + " chestSlot=" + event.getSlot()
                + " lootSlot=" + ref.slotIndex()
                + " item=" + stack.getType().name() + "x" + stack.getAmount());
        if (!lootGuard.tryTakeObfuscated(player, stack, ref.sessionId(), ref.slotIndex())) {
            if (lootGuard.isSlotClaimed(ref.sessionId(), ref.slotIndex())) {
                service.messages().send(player, "airdrop.chest-slot-taken", Map.of());
            } else {
                service.messages().send(player, "airdrop.chest-open-failed", Map.of());
            }
            return;
        }
        lootGuard.registerTake(player, ref.sessionId(), ref.slotIndex());
        top.setItem(event.getSlot(), new ItemStack(Material.AIR));
        service.scheduleEmptyCheck(chest.sessionId(), player);
    }

    private void sanitizeChestInventory(AirDropChestHolder chest, Inventory top) {
        LootGuardService lootGuard = service.lootGuard();
        for (int slot = 0; slot < top.getSize(); slot++) {
            ItemStack stack = top.getItem(slot);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Optional<ObfuscatedLootRef> refOptional = lootGuard.obfuscatedRef(stack);
            if (refOptional.isPresent()
                    && refOptional.get().sessionId().equals(chest.sessionId())) {
                continue;
            }
            top.setItem(slot, new ItemStack(Material.AIR));
            lootGuard.debugWarn("sanitizeChest removed foreign slot=" + slot
                    + " session=" + chest.sessionId()
                    + " item=" + stack.getType().name());
        }
    }

    private void revealCarriedObfuscated(Player player) {
        LootGuardService lootGuard = service.lootGuard();
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Optional<ObfuscatedLootRef> refOptional = lootGuard.obfuscatedRef(stack);
            if (refOptional.isEmpty()) {
                continue;
            }
            ObfuscatedLootRef ref = refOptional.get();
            lootGuard.resumePendingReveal(player, ref);
        }
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand != null && !offhand.getType().isAir()) {
            lootGuard.obfuscatedRef(offhand).ifPresent(ref -> lootGuard.resumePendingReveal(player, ref));
        }
    }

    private void handleInventoryChange(org.bukkit.entity.HumanEntity viewer, Inventory inventory) {
        if (!(viewer instanceof Player player)) {
            return;
        }
        Optional<UUID> sessionId = sessionIdForInventory(inventory);
        if (sessionId.isEmpty()) {
            return;
        }
        service.scheduleEmptyCheck(sessionId.get(), player);
    }

    private Optional<UUID> sessionIdForInventory(Inventory inventory) {
        InventoryHolder holder = inventory.getHolder(false);
        if (holder instanceof AirDropChestHolder chest) {
            return Optional.of(chest.sessionId());
        }
        if (holder instanceof BlockState state) {
            return service.sessionIdAt(state.getLocation());
        }
        return Optional.empty();
    }

    private void removeAnchorBlocks(Iterator<Block> blocks) {
        while (blocks.hasNext()) {
            if (service.sessionIdAt(blocks.next().getLocation()).isPresent()) {
                blocks.remove();
            }
        }
    }
}
