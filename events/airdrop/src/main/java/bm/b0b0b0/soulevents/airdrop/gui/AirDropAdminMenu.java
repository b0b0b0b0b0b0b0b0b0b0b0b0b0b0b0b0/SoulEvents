package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.AdminHubGuiSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.TypeListGuiSettings;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AirDropAdminMenu implements InventoryHolder {

    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropGuiFactory guiFactory;
    private final Inventory inventory;
    private final Map<Integer, String> slotTypes = new HashMap<>();

    public AirDropAdminMenu(AirDropPluginConfig config, AirDropMessageService messages, AirDropGuiFactory guiFactory) {
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        AdminHubGuiSettings hub = config.gui().adminHub;
        this.inventory = Bukkit.createInventory(this, hub.rows * 9, messages.resolve("gui.admin.title", Map.of()));
        render();
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
        AdminHubGuiSettings hub = config.gui().adminHub;
        if (event.getClickedInventory() == null || event.getRawSlot() != event.getSlot()) {
            return;
        }
        if (event.getRawSlot() == hub.closeSlot) {
            player.closeInventory();
            return;
        }
        if (event.getRawSlot() == hub.createSlot) {
            guiFactory.openCreate(player);
            return;
        }
        String typeId = slotTypes.get(event.getRawSlot());
        if (typeId == null) {
            return;
        }
        if (event.getClick() == ClickType.LEFT) {
            guiFactory.openTypeSettings(player, typeId);
        }
    }

    private void render() {
        inventory.clear();
        slotTypes.clear();
        AdminHubGuiSettings hub = config.gui().adminHub;
        TypeListGuiSettings list = config.gui().typeList;
        inventory.setItem(hub.closeSlot, icon(
                Material.matchMaterial(hub.closeMaterial),
                messages.resolve("gui.admin.close", Map.of()),
                List.of()
        ));
        inventory.setItem(hub.createSlot, icon(
                Material.matchMaterial(hub.createMaterial),
                messages.resolve("gui.admin.create", Map.of()),
                List.of(messages.resolve("gui.admin.create-lore", Map.of()))
        ));
        int slot = list.startSlot;
        for (AirDropTypeDefinition definition : config.types()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            Material material = definition.settings().enabled
                    ? Material.matchMaterial(list.defaultIconMaterial)
                    : Material.matchMaterial(list.disabledIconMaterial);
            List<Component> lore = new ArrayList<>();
            lore.add(messages.resolve(
                    definition.settings().enabled ? "gui.admin.type-enabled" : "gui.admin.type-disabled",
                    Map.of()
            ));
            lore.add(messages.resolve("gui.admin.type-lore-interval", Map.of(
                    "interval", Long.toString(definition.settings().intervalMinutes)
            )));
            lore.add(messages.resolve("gui.admin.type-lore-world", Map.of(
                    "world", definition.settings().worldPlacement.spawnWorld
            )));
            lore.add(messages.resolve("gui.admin.type-lore-loot", Map.of(
                    "loot", definition.lootTableId()
            )));
            lore.add(messages.resolve("gui.admin.type-lore-open", Map.of()));
            inventory.setItem(slot, icon(
                    material,
                    messages.resolve(definition.settings().displayNameKey, Map.of()),
                    lore
            ));
            slotTypes.put(slot, definition.id());
            slot++;
        }
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
