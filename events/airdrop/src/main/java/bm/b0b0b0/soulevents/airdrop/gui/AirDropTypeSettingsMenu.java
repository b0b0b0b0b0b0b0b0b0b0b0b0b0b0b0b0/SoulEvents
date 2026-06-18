package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeSettingsGuiSettings;
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

import java.util.ArrayList;
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
        if (event.getClickedInventory() == null || event.getRawSlot() != event.getSlot()) {
            return;
        }
        TypeSettingsGuiSettings gui = config.gui().typeSettings;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openAdmin(player);
            return;
        }
        if (slot == gui.summonSlot) {
            player.closeInventory();
            service.spawnAdminAsync(player, typeId);
            return;
        }
        if (slot == gui.teleportSlot) {
            player.closeInventory();
            service.teleportToActive(player, typeId);
            return;
        }
        if (slot == gui.multiSummonSlot) {
            player.closeInventory();
            service.spawnAdminBatchAsync(player, typeId, gui.multiSummonCount);
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

        inventory.setItem(gui.backSlot, icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.type-settings.back", Map.of()),
                List.of()
        ));

        List<Component> summonLore = new ArrayList<>();
        summonLore.add(messages.resolve("gui.type-settings.summon-lore-world", Map.of("world", spawnWorld)));
        summonLore.add(messages.resolve("gui.type-settings.summon-lore-action", Map.of()));
        inventory.setItem(gui.summonSlot, icon(
                Material.matchMaterial(gui.summonMaterial),
                messages.resolve("gui.type-settings.summon", Map.of()),
                summonLore
        ));

        List<Component> teleportLore = new ArrayList<>();
        teleportLore.add(messages.resolve("gui.type-settings.teleport-lore-action", Map.of()));
        inventory.setItem(gui.teleportSlot, icon(
                Material.matchMaterial(gui.teleportMaterial),
                messages.resolve("gui.type-settings.teleport", Map.of()),
                teleportLore
        ));

        List<Component> batchLore = new ArrayList<>();
        batchLore.add(messages.resolve("gui.type-settings.multi-summon-lore-count", Map.of(
                "count", Integer.toString(gui.multiSummonCount)
        )));
        batchLore.add(messages.resolve("gui.type-settings.multi-summon-lore-action", Map.of()));
        inventory.setItem(gui.multiSummonSlot, icon(
                Material.matchMaterial(gui.multiSummonMaterial),
                messages.resolve("gui.type-settings.multi-summon", Map.of()),
                batchLore
        ));

        List<Component> lootLore = new ArrayList<>();
        lootLore.add(messages.resolve("gui.type-settings.loot-lore-file", Map.of(
                "loot", definition.lootTableId()
        )));
        lootLore.add(messages.resolve("gui.type-settings.loot-lore-hint", Map.of()));
        inventory.setItem(gui.lootInfoSlot, icon(
                Material.matchMaterial(gui.lootInfoMaterial),
                messages.resolve(definition.settings().displayNameKey, Map.of()),
                lootLore
        ));
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
