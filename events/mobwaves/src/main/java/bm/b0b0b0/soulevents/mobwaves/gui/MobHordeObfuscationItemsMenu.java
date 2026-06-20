package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.LootConfigPersistence;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.ObfuscationItemsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.SerializedItemStackCodec;
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

public final class MobHordeObfuscationItemsMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;
    private final int[] maskSlots;
    private boolean skipCloseSave;

    public MobHordeObfuscationItemsMenu(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String typeId
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.typeId = typeId;
        ObfuscationItemsGuiSettings gui = config.gui().obfuscationItems;
        this.maskSlots = new int[] {gui.maskSlotLeft, gui.maskSlotCenter, gui.maskSlotRight};
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.obfuscation-items.title", Map.of("type", typeId))
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
        for (int maskSlot : maskSlots) {
            if (maskSlot == rawSlot) {
                return true;
            }
        }
        return false;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            event.setCancelled(true);
            return;
        }
        ObfuscationItemsGuiSettings gui = config.gui().obfuscationItems;
        int rawSlot = event.getRawSlot();
        if (rawSlot == gui.backSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persistItems(player);
            guiFactory.openLootHub(player, typeId);
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
        Optional<HordeTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        HordeTypeDefinition definition = definitionOptional.get();
        LootTableSettings loot = definition.loot();
        List<String> encoded = new ArrayList<>(loot.obfuscationItemsBase64);
        while (encoded.size() < maskSlots.length) {
            encoded.add("");
        }
        for (int index = 0; index < maskSlots.length; index++) {
            ItemStack stack = inventory.getItem(maskSlots[index]);
            String value = SerializedItemStackCodec.encode(stack);
            encoded.set(index, value == null ? "" : value);
            inventory.setItem(maskSlots[index], null);
        }
        if (encoded.size() > maskSlots.length) {
            encoded = new ArrayList<>(encoded.subList(0, maskSlots.length));
        }
        loot.obfuscationItemsBase64 = encoded;
        LootConfigPersistence.saveLootTable(plugin, definition.lootTableId(), loot);
        messages.send(player, "gui.obfuscation-items.saved", Map.of(
                "count", Integer.toString(SerializedItemStackCodec.countEncodedEntries(encoded))
        ));
    }

    private void loadItems() {
        Optional<HordeTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        List<String> encoded = definitionOptional.get().loot().obfuscationItemsBase64;
        for (int index = 0; index < maskSlots.length; index++) {
            if (encoded == null || index >= encoded.size()) {
                continue;
            }
            ItemStack stack = SerializedItemStackCodec.decode(encoded.get(index));
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            inventory.setItem(maskSlots[index], stack.clone());
        }
    }

    private void renderFrame() {
        ObfuscationItemsGuiSettings gui = config.gui().obfuscationItems;
        Material fillerMaterial = Material.matchMaterial(gui.fillerMaterial);
        ItemStack filler = filler(fillerMaterial);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (slot == gui.backSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.backMaterial),
                        messages.resolve("gui.obfuscation-items.back", Map.of("type", typeId)),
                        messages.resolveLore("gui.obfuscation-items.back-lore", Map.of("type", typeId))
                ));
                continue;
            }
            if (slot == gui.guideSlot) {
                inventory.setItem(slot, icon(
                        Material.matchMaterial(gui.guideMaterial),
                        messages.resolve("gui.obfuscation-items.guide", Map.of("type", typeId)),
                        messages.resolveLore("gui.obfuscation-items.guide-lore", Map.of("type", typeId))
                ));
                continue;
            }
            if (isEditableSlot(slot)) {
                continue;
            }
            inventory.setItem(slot, filler.clone());
        }
    }

    private static ItemStack filler(Material material) {
        ItemStack stack = new ItemStack(material == null ? Material.BARRIER : material);
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

