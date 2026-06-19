package bm.b0b0b0.soulevents.volcano.gui;

import bm.b0b0b0.soulevents.volcano.VolcanoPlugin;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPluginConfig;
import bm.b0b0b0.soulevents.volcano.config.TypeCreatorService;
import bm.b0b0b0.soulevents.volcano.config.settings.CreateGuiSettings;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
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

import java.util.List;
import java.util.Map;

public final class VolcanoCreateMenu implements InventoryHolder {

    private final VolcanoPlugin plugin;
    private final VolcanoPluginConfig config;
    private final VolcanoMessageService messages;
    private final VolcanoGuiFactory guiFactory;
    private final TypeCreatorService typeCreator;
    private final Inventory inventory;

    public VolcanoCreateMenu(
            VolcanoPlugin plugin,
            VolcanoPluginConfig config,
            VolcanoMessageService messages,
            VolcanoGuiFactory guiFactory
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.typeCreator = new TypeCreatorService(plugin);
        CreateGuiSettings gui = config.gui().create;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.create.title", Map.of())
        );
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
        if (event.getClickedInventory() == null || event.getRawSlot() != event.getSlot()) {
            return;
        }
        CreateGuiSettings gui = config.gui().create;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openAdmin(player);
            return;
        }
        TypeCreatorService.Preset preset = presetForSlot(slot, gui);
        if (preset == null) {
            return;
        }
        String typeId = typeCreator.createFromPreset(preset);
        plugin.reloadAll();
        messages.send(player, "gui.create.created", Map.of("type", typeId));
        guiFactory.openTypeSettings(player, typeId);
    }

    private TypeCreatorService.Preset presetForSlot(int slot, CreateGuiSettings gui) {
        if (slot == gui.commonSlot) {
            return TypeCreatorService.Preset.COMMON;
        }
        if (slot == gui.rareSlot) {
            return TypeCreatorService.Preset.RARE;
        }
        if (slot == gui.donateSlot) {
            return TypeCreatorService.Preset.DONATE;
        }
        return null;
    }

    private void render() {
        inventory.clear();
        CreateGuiSettings gui = config.gui().create;
        inventory.setItem(gui.backSlot, icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.create.back", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.commonSlot, icon(
                Material.matchMaterial(gui.commonMaterial),
                messages.resolve("gui.create.common", Map.of()),
                messages.resolveLore("gui.create.common-lore", Map.of())
        ));
        inventory.setItem(gui.rareSlot, icon(
                Material.matchMaterial(gui.rareMaterial),
                messages.resolve("gui.create.rare", Map.of()),
                messages.resolveLore("gui.create.rare-lore", Map.of())
        ));
        inventory.setItem(gui.donateSlot, icon(
                Material.matchMaterial(gui.donateMaterial),
                messages.resolve("gui.create.donate", Map.of()),
                messages.resolveLore("gui.create.donate-lore", Map.of())
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

