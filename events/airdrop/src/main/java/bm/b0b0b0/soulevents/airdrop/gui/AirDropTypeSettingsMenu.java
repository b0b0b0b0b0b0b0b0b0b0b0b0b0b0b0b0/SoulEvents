package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeSettingsGuiSettings;
import bm.b0b0b0.soulevents.airdrop.util.SerializedItemStackCodec;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
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

public final class AirDropTypeSettingsMenu implements InventoryHolder {

    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropService service;
    private final AirDropGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;

    public AirDropTypeSettingsMenu(
            AirDropPluginConfig config,
            AirDropMessageService messages,
            AirDropService service,
            AirDropGuiFactory guiFactory,
            String typeId
    ) {
        this.config = config;
        this.messages = messages;
        this.service = service;
        this.guiFactory = guiFactory;
        this.typeId = typeId;
        TypeSettingsGuiSettings gui = config.gui().typeSettings;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.type-settings.title", Map.of("type", typeId))
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
        TypeSettingsGuiSettings gui = config.gui().typeSettings;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openAdmin(player);
            return;
        }
        if (slot == gui.summonSlot) {
            service.spawnAdminAsync(player, typeId);
            return;
        }
        if (slot == gui.teleportSlot) {
            service.teleportToActive(player, typeId);
            return;
        }
        if (slot == gui.multiSummonSlot) {
            service.spawnAdminBatchAsync(player, typeId, gui.multiSummonCount);
            return;
        }
        if (slot == gui.despawnSlot) {
            service.despawnAdmin(player, typeId);
            render();
            return;
        }
        if (slot == gui.requirementsSlot) {
            if (event.isRightClick()) {
                guiFactory.openRequiredItems(player, typeId);
                return;
            }
            if (event.isLeftClick()) {
                guiFactory.openRequirements(player, typeId);
            }
            return;
        }
        if (slot == gui.lootInfoSlot) {
            guiFactory.openLootHub(player, typeId);
        }
    }

    private void render() {
        inventory.clear();
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeDefinition definition = definitionOptional.get();
        TypeSettingsGuiSettings gui = config.gui().typeSettings;
        String spawnWorld = definition.settings().worldPlacement.spawnWorld;

        Map<String, String> ph = typePlaceholders(definition, spawnWorld, gui);

        inventory.setItem(gui.backSlot, icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.type-settings.back", ph),
                messages.resolveLore("gui.type-settings.back-lore", ph)
        ));
        inventory.setItem(gui.summonSlot, icon(
                Material.matchMaterial(gui.summonMaterial),
                messages.resolve("gui.type-settings.summon", ph),
                messages.resolveLore("gui.type-settings.summon-lore", ph)
        ));
        inventory.setItem(gui.teleportSlot, icon(
                Material.matchMaterial(gui.teleportMaterial),
                messages.resolve("gui.type-settings.teleport", ph),
                messages.resolveLore("gui.type-settings.teleport-lore", ph)
        ));
        inventory.setItem(gui.multiSummonSlot, icon(
                Material.matchMaterial(gui.multiSummonMaterial),
                messages.resolve("gui.type-settings.multi-summon", ph),
                messages.resolveLore("gui.type-settings.multi-summon-lore", ph)
        ));
        inventory.setItem(gui.despawnSlot, icon(
                Material.matchMaterial(gui.despawnMaterial),
                messages.resolve("gui.type-settings.despawn", ph),
                messages.resolveLore("gui.type-settings.despawn-lore", ph)
        ));
        inventory.setItem(gui.lootInfoSlot, icon(
                Material.matchMaterial(gui.lootInfoMaterial),
                messages.resolve("gui.type-settings.loot", ph),
                messages.resolveLore("gui.type-settings.loot-lore", ph)
        ));
        inventory.setItem(gui.requirementsSlot, icon(
                Material.matchMaterial(gui.requirementsMaterial),
                messages.resolve("gui.type-settings.requirements", ph),
                messages.resolveLore("gui.type-settings.requirements-lore", ph)
        ));
    }

    private Map<String, String> typePlaceholders(
            AirDropTypeDefinition definition,
            String spawnWorld,
            TypeSettingsGuiSettings gui
    ) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("world", spawnWorld);
        values.put("loot", definition.lootTableId());
        values.put("count", Integer.toString(SerializedItemStackCodec.countEncodedEntries(
                definition.loot().poolItemsBase64
        )));
        values.put("occupied", Integer.toString(definition.loot().occupiedSlots));
        values.put("batch", Integer.toString(gui.multiSummonCount));
        values.put("active", Integer.toString(service.countActive(typeId)));
        values.put("permission_state", definition.settings().openPermission.enabled
                ? messages.resolvePlain("gui.requirements.state-on", Map.of())
                : messages.resolvePlain("gui.requirements.state-off", Map.of()));
        values.put("item_state", definition.settings().requiredLoot.enabled
                ? messages.resolvePlain("gui.requirements.state-on", Map.of())
                : messages.resolvePlain("gui.requirements.state-off", Map.of()));
        values.put("templates", Integer.toString(SerializedItemStackCodec.countEncodedEntries(
                definition.settings().requiredLoot.requiredItemsBase64
        )));
        values.put("mode", definition.settings().requiredLoot.isAnyMatch()
                ? messages.resolvePlain("gui.requirements.match-mode-any-short", Map.of())
                : messages.resolvePlain("gui.requirements.match-mode-all-short", Map.of()));
        return values;
    }

    private ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack stack = new ItemStack(material == null ? Material.CHEST : material);
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
