package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.TypeConfigPersistence;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.RequiredItemsGuiSettings;
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

public final class AirDropRequiredItemsMenu implements InventoryHolder {

    private final AirDropPlugin plugin;
    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;
    private final int editableSlotCount;
    private boolean skipCloseSave;

    public AirDropRequiredItemsMenu(
            AirDropPlugin plugin,
            AirDropPluginConfig config,
            AirDropMessageService messages,
            AirDropGuiFactory guiFactory,
            String typeId
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.typeId = typeId;
        RequiredItemsGuiSettings gui = config.gui().requiredItems;
        this.editableSlotCount = Math.min(gui.rows * 9, Math.max(1, gui.editableSlotCount));
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.required-items.title", Map.of("type", typeId))
        );
        renderFrame();
        loadItems();
    }

    public String typeId() {
        return typeId;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public boolean isEditableSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < editableSlotCount;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        RequiredItemsGuiSettings gui = config.gui().requiredItems;
        int rawSlot = event.getRawSlot();
        if (rawSlot == gui.backSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persistItems(player);
            guiFactory.openRequirements(player, typeId);
            return;
        }
        if (rawSlot == gui.guideSlot) {
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
        persistItems(player);
    }

    private void persistItems(Player player) {
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeSettings settings = definitionOptional.get().settings();
        List<String> encoded = SerializedItemStackCodec.encodeSlotItems(
                editableSlotCount,
                slot -> inventory.getItem(slot)
        );
        for (int slot = 0; slot < editableSlotCount; slot++) {
            inventory.setItem(slot, null);
        }
        SerializedItemStackCodec.trimTrailingEmptyEntries(encoded);
        settings.requiredLoot.requiredItemsBase64 = encoded;
        TypeConfigPersistence.saveTypeSettings(plugin, typeId, settings);
        messages.send(player, "gui.required-items.saved", Map.of(
                "count", Integer.toString(SerializedItemStackCodec.countEncodedEntries(encoded))
        ));
    }

    private void loadItems() {
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        SerializedItemStackCodec.decodeIntoSlots(
                definitionOptional.get().settings().requiredLoot.requiredItemsBase64,
                editableSlotCount,
                inventory::setItem
        );
    }

    private void renderFrame() {
        RequiredItemsGuiSettings gui = config.gui().requiredItems;
        Material fillerMaterial = Material.matchMaterial(gui.fillerMaterial);
        ItemStack filler = filler(fillerMaterial);
        for (int slot = editableSlotCount; slot < inventory.getSize(); slot++) {
            if (slot == gui.backSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.backMaterial),
                        messages.resolve("gui.required-items.back", Map.of("type", typeId)),
                        messages.resolveLore("gui.required-items.back-lore", Map.of("type", typeId))
                ));
                continue;
            }
            if (slot == gui.guideSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.guideMaterial),
                        messages.resolve("gui.required-items.guide", Map.of("type", typeId)),
                        messages.resolveLore("gui.required-items.guide-lore", Map.of("type", typeId))
                ));
                continue;
            }
            inventory.setItem(slot, filler.clone());
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
        ItemStack stack = new ItemStack(material == null ? Material.LIME_DYE : material);
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
