package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.TypeConfigPersistence;
import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropTypeSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.RequirementsGuiSettings;
import bm.b0b0b0.soulevents.airdrop.util.SerializedItemStackCodec;
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

import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AirDropRequirementsMenu implements InventoryHolder {

    private final AirDropPlugin plugin;
    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;

    public AirDropRequirementsMenu(
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
        RequirementsGuiSettings gui = config.gui().requirements;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.requirements.title", Map.of("type", typeId))
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
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeSettings settings = definitionOptional.get().settings();
        RequirementsGuiSettings gui = config.gui().requirements;
        int slot = event.getRawSlot();
        if (slot == gui.guideSlot) {
            return;
        }
        if (slot == gui.backSlot) {
            guiFactory.openTypeSettings(player, typeId);
            return;
        }
        if (slot == gui.permissionToggleSlot) {
            if (!event.isLeftClick() || event.isShiftClick()) {
                return;
            }
            settings.openPermission.enabled = !settings.openPermission.enabled;
            persist(settings);
            render();
            messages.send(player, settings.openPermission.enabled
                    ? "gui.requirements.permission-enabled"
                    : "gui.requirements.permission-disabled", Map.of());
            return;
        }
        if (slot == gui.matchModeSlot) {
            if (!event.isLeftClick() || event.isShiftClick()) {
                return;
            }
            settings.requiredLoot.cycleMatchMode();
            persist(settings);
            render();
            messages.send(player, settings.requiredLoot.isAnyMatch()
                    ? "gui.requirements.match-any"
                    : "gui.requirements.match-all", Map.of());
            return;
        }
        if (slot == gui.customItemToggleSlot) {
            if (event.isRightClick()) {
                guiFactory.openRequiredItems(player, typeId);
                return;
            }
            if (!event.isLeftClick() || event.isShiftClick()) {
                return;
            }
            settings.requiredLoot.enabled = !settings.requiredLoot.enabled;
            persist(settings);
            render();
            messages.send(player, settings.requiredLoot.enabled
                    ? "gui.requirements.item-enabled"
                    : "gui.requirements.item-disabled", Map.of());
        }
    }

    private void persist(AirDropTypeSettings settings) {
        TypeConfigPersistence.saveTypeSettings(plugin, typeId, settings);
    }

    private void render() {
        inventory.clear();
        Optional<AirDropTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        AirDropTypeSettings settings = definitionOptional.get().settings();
        RequirementsGuiSettings gui = config.gui().requirements;
        Map<String, String> ph = requirementPlaceholders(settings);

        Material fillerMaterial = Material.matchMaterial(gui.fillerMaterial);
        ItemStack filler = filler(fillerMaterial);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler.clone());
        }

        inventory.setItem(gui.guideSlot, icon(
                Material.matchMaterial(gui.guideMaterial),
                messages.resolve("gui.requirements.guide", ph),
                messages.resolveLore("gui.requirements.guide-lore", ph)
        ));
        inventory.setItem(gui.backSlot, icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.requirements.back", ph),
                messages.resolveLore("gui.requirements.back-lore", ph)
        ));
        inventory.setItem(gui.permissionToggleSlot, icon(
                Material.matchMaterial(gui.permissionToggleMaterial),
                messages.resolve("gui.requirements.permission-toggle", ph),
                messages.resolveLore("gui.requirements.permission-lore", ph)
        ));
        inventory.setItem(gui.matchModeSlot, icon(
                Material.matchMaterial(gui.matchModeMaterial),
                messages.resolve("gui.requirements.match-mode-toggle", ph),
                messages.resolveLore("gui.requirements.match-mode-lore", ph)
        ));
        inventory.setItem(gui.customItemToggleSlot, icon(
                Material.matchMaterial(gui.customItemToggleMaterial),
                messages.resolve("gui.requirements.item-toggle", ph),
                messages.resolveLore("gui.requirements.item-lore", ph)
        ));
    }

    private Map<String, String> requirementPlaceholders(AirDropTypeSettings settings) {
        Map<String, String> values = new HashMap<>();
        String permissionNode = TypeConfigPersistence.resolveOpenPermission(typeId, settings.openPermission.permission);
        values.put("type", typeId);
        values.put("permission", permissionNode);
        values.put("permission_state", settings.openPermission.enabled
                ? messages.resolvePlain("gui.requirements.state-on", Map.of())
                : messages.resolvePlain("gui.requirements.state-off", Map.of()));
        values.put("item_state", settings.requiredLoot.enabled
                ? messages.resolvePlain("gui.requirements.state-on", Map.of())
                : messages.resolvePlain("gui.requirements.state-off", Map.of()));
        values.put("count", Integer.toString(SerializedItemStackCodec.countEncodedEntries(
                settings.requiredLoot.requiredItemsBase64
        )));
        values.put("mode", settings.requiredLoot.isAnyMatch()
                ? messages.resolvePlain("gui.requirements.match-mode-any-short", Map.of())
                : messages.resolvePlain("gui.requirements.match-mode-all-short", Map.of()));
        values.put("mode_long", settings.requiredLoot.isAnyMatch()
                ? messages.resolvePlain("gui.requirements.match-mode-any", Map.of())
                : messages.resolvePlain("gui.requirements.match-mode-all", Map.of()));
        return values;
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
