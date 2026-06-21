package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.LootConfigPersistence;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootHubGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.LootTableSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.SerializedItemStackCodec;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobHordeLootHubMenu implements InventoryHolder {

    private static final int MAX_POOL_ROLLS = 9;

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;

    public MobHordeLootHubMenu(
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
        LootHubGuiSettings gui = config.gui().lootHub;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.loot-hub.title", Map.of("type", typeId))
        );
        render();
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

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() != inventory || event.getRawSlot() != event.getSlot()) {
            return;
        }
        Optional<HordeTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        LootTableSettings loot = definitionOptional.get().loot();
        LootHubGuiSettings gui = config.gui().lootHub;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openTypeSettings(player, typeId);
            return;
        }
        if (slot == gui.infoSlot) {
            return;
        }
        if (slot == gui.obfuscationSlot) {
            guiFactory.openObfuscationItems(player, typeId);
            return;
        }
        if (slot == gui.poolSlot) {
            guiFactory.openLootPool(player, typeId, 0);
            return;
        }
        int maxOccupied = MAX_POOL_ROLLS;
        if (slot == gui.occupiedMinusSlot) {
            loot.occupiedSlots = Math.max(0, loot.occupiedSlots - 1);
            persist(loot, definitionOptional.get());
            render();
            messages.send(player, "gui.loot-hub.occupied-changed", placeholders(loot, definitionOptional.get()));
            return;
        }
        if (slot == gui.occupiedPlusSlot) {
            loot.occupiedSlots = Math.min(maxOccupied, loot.occupiedSlots + 1);
            persist(loot, definitionOptional.get());
            render();
            messages.send(player, "gui.loot-hub.occupied-changed", placeholders(loot, definitionOptional.get()));
        }
    }

    private void persist(LootTableSettings loot, HordeTypeDefinition definition) {
        LootConfigPersistence.saveLootTable(plugin, definition.lootTableId(), loot);
    }

    private Map<String, String> placeholders(LootTableSettings loot, HordeTypeDefinition definition) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("loot", definition.lootTableId());
        values.put("count", Integer.toString(loot.occupiedSlots));
        values.put("max", Integer.toString(MAX_POOL_ROLLS));
        values.put("pool", Integer.toString(SerializedItemStackCodec.countEncodedEntries(loot.poolItemsBase64)));
        values.put("masks", Integer.toString(SerializedItemStackCodec.countEncodedEntries(loot.obfuscationItemsBase64)));
        return values;
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        Optional<HordeTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        HordeTypeDefinition definition = definitionOptional.get();
        LootTableSettings loot = definition.loot();
        LootHubGuiSettings gui = config.gui().lootHub;
        Map<String, String> ph = placeholders(loot, definition);

        inventory.setItem(gui.infoSlot, icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.loot-hub.info", ph),
                messages.resolveLore("gui.loot-hub.info-lore", ph)
        ));
        inventory.setItem(gui.backSlot, icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.loot-hub.back", ph),
                messages.resolveLore("gui.loot-hub.back-lore", ph)
        ));
        inventory.setItem(gui.obfuscationSlot, icon(
                Material.matchMaterial(gui.obfuscationMaterial),
                messages.resolve("gui.loot-hub.obfuscation", ph),
                messages.resolveLore("gui.loot-hub.obfuscation-lore", ph)
        ));
        inventory.setItem(gui.poolSlot, icon(
                Material.matchMaterial(gui.poolMaterial),
                messages.resolve("gui.loot-hub.pool", ph),
                messages.resolveLore("gui.loot-hub.pool-lore", ph)
        ));
        inventory.setItem(gui.occupiedInfoSlot, icon(
                Material.matchMaterial(gui.occupiedInfoMaterial),
                messages.resolve("gui.loot-hub.occupied", ph),
                messages.resolveLore("gui.loot-hub.occupied-lore", ph)
        ));
        inventory.setItem(gui.occupiedMinusSlot, icon(
                Material.matchMaterial(gui.occupiedMinusMaterial),
                messages.resolve("gui.loot-hub.occupied-minus", ph),
                messages.resolveLore("gui.loot-hub.occupied-minus-lore", ph)
        ));
        inventory.setItem(gui.occupiedPlusSlot, icon(
                Material.matchMaterial(gui.occupiedPlusMaterial),
                messages.resolve("gui.loot-hub.occupied-plus", ph),
                messages.resolveLore("gui.loot-hub.occupied-plus-lore", ph)
        ));
    }

    private ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.PAPER : material);
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

