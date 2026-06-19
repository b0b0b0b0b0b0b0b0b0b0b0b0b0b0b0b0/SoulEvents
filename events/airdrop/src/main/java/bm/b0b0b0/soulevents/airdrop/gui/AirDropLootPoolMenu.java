package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.LootConfigPersistence;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootPoolGuiSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.util.SerializedItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AirDropLootPoolMenu implements InventoryHolder {

    private final AirDropPlugin plugin;
    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropGuiFactory guiFactory;
    private final String typeId;
    private final int page;
    private final Inventory inventory;
    private final int editableSlotCount;
    private boolean skipCloseSave;

    public AirDropLootPoolMenu(
            AirDropPlugin plugin,
            AirDropPluginConfig config,
            AirDropMessageService messages,
            AirDropGuiFactory guiFactory,
            String typeId,
            int page
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.typeId = typeId;
        LootPoolGuiSettings gui = config.gui().lootPool;
        this.page = Math.max(0, Math.min(gui.maxPages - 1, page));
        this.editableSlotCount = Math.min(gui.rows * 9, Math.max(1, gui.editableSlotCount));
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.loot-pool.title", Map.of(
                        "type", typeId,
                        "page", Integer.toString(this.page + 1),
                        "pages", Integer.toString(gui.maxPages)
                ))
        );
        renderFrame();
        loadPage();
    }

    public String typeId() {
        return typeId;
    }

    public int page() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public boolean isEditableSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < editableSlotCount && !isFrameSlot(rawSlot);
    }

    private boolean isFrameSlot(int rawSlot) {
        LootPoolGuiSettings gui = config.gui().lootPool;
        return rawSlot == gui.backSlot
                || rawSlot == gui.prevPageSlot
                || rawSlot == gui.nextPageSlot
                || rawSlot == gui.pageInfoSlot;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        LootPoolGuiSettings gui = config.gui().lootPool;
        int rawSlot = event.getRawSlot();
        if (rawSlot == gui.backSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persistPage(player);
            guiFactory.openLootHub(player, typeId);
            return;
        }
        if (rawSlot == gui.prevPageSlot && page > 0) {
            event.setCancelled(true);
            skipCloseSave = true;
            persistPage(player);
            guiFactory.openLootPool(player, typeId, page - 1);
            return;
        }
        if (rawSlot == gui.nextPageSlot && page + 1 < gui.maxPages) {
            event.setCancelled(true);
            skipCloseSave = true;
            persistPage(player);
            guiFactory.openLootPool(player, typeId, page + 1);
            return;
        }
        if (isFrameSlot(rawSlot)) {
            event.setCancelled(true);
            return;
        }
        if (rawSlot >= inventory.getSize()) {
            return;
        }
        if (!isEditableSlot(rawSlot)) {
            event.setCancelled(true);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize() && !isEditableSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    public void handleClose(Player player) {
        if (skipCloseSave) {
            return;
        }
        persistPage(player);
    }

    private void persistPage(Player player) {
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeDefinition definition = definitionOptional.get();
        LootTableSettings loot = definition.loot();
        List<String> encoded = new ArrayList<>(loot.poolItemsBase64);
        int pageStart = page * editableSlotCount;
        ensureEncodedSize(encoded, pageStart + editableSlotCount);
        int savedOnPage = 0;
        for (int slot = 0; slot < editableSlotCount; slot++) {
            ItemStack stack = inventory.getItem(slot);
            int targetIndex = pageStart + slot;
            String value = SerializedItemStackCodec.encode(stack);
            encoded.set(targetIndex, value == null ? "" : value);
            if (value != null) {
                savedOnPage++;
            }
            inventory.setItem(slot, null);
        }
        SerializedItemStackCodec.trimTrailingPoolPages(encoded, editableSlotCount);
        loot.poolItemsBase64 = encoded;
        LootConfigPersistence.saveLootTable(plugin, definition.lootTableId(), loot);
        messages.send(player, "gui.loot-pool.saved", Map.of(
                "page", Integer.toString(page + 1),
                "page_count", Integer.toString(savedOnPage),
                "total", Integer.toString(SerializedItemStackCodec.countEncodedEntries(encoded))
        ));
    }

    private void loadPage() {
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        SerializedItemStackCodec.decodeIntoSlots(
                definitionOptional.get().loot().poolItemsBase64,
                page * editableSlotCount,
                editableSlotCount,
                inventory::setItem
        );
    }

    private void renderFrame() {
        LootPoolGuiSettings gui = config.gui().lootPool;
        Map<String, String> ph = Map.of(
                "type", typeId,
                "page", Integer.toString(page + 1),
                "pages", Integer.toString(gui.maxPages)
        );
        Material fillerMaterial = Material.matchMaterial(gui.fillerMaterial);
        ItemStack filler = filler(fillerMaterial);
        for (int slot = editableSlotCount; slot < inventory.getSize(); slot++) {
            if (slot == gui.backSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.backMaterial),
                        messages.resolve("gui.loot-pool.back", ph),
                        messages.resolveLore("gui.loot-pool.back-lore", ph)
                ));
                continue;
            }
            if (slot == gui.prevPageSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.prevPageMaterial),
                        messages.resolve("gui.loot-pool.prev", ph),
                        messages.resolveLore("gui.loot-pool.prev-lore", ph)
                ));
                continue;
            }
            if (slot == gui.nextPageSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.nextPageMaterial),
                        messages.resolve("gui.loot-pool.next", ph),
                        messages.resolveLore("gui.loot-pool.next-lore", ph)
                ));
                continue;
            }
            if (slot == gui.pageInfoSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.pageInfoMaterial),
                        messages.resolve("gui.loot-pool.page-info", ph),
                        messages.resolveLore("gui.loot-pool.page-info-lore", ph)
                ));
                continue;
            }
            inventory.setItem(slot, filler.clone());
        }
    }

    private static void ensureEncodedSize(List<String> encoded, int size) {
        while (encoded.size() < size) {
            encoded.add("");
        }
    }

    private static ItemStack filler(Material material) {
        ItemStack stack = new ItemStack(material == null ? Material.GRAY_STAINED_GLASS_PANE : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.ARROW : material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(name);
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
