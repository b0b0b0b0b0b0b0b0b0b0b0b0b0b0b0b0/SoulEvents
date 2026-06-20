package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.TypeSettingsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;
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

public final class MobHordeTypeSettingsMenu implements InventoryHolder {

    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobHordeService service;
    private final MobWavesGuiFactory guiFactory;
    private final String typeId;
    private final Inventory inventory;

    public MobHordeTypeSettingsMenu(
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobHordeService service,
            MobWavesGuiFactory guiFactory,
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
        if (slot == gui.despawnSlot) {
            service.despawnAdmin(player, typeId);
            render();
            return;
        }
        if (slot == gui.waveProfileSlot) {
            config.type(typeId).ifPresent(definition ->
                    guiFactory.openProfile(player, definition.settings().waveProfileId)
            );
            return;
        }
        if (slot == gui.lootInfoSlot) {
            guiFactory.openLootHub(player, typeId);
        }
    }

    private void render() {
        inventory.clear();
        Optional<HordeTypeDefinition> definitionOptional = config.type(typeId);
        if (definitionOptional.isEmpty()) {
            return;
        }
        HordeTypeDefinition definition = definitionOptional.get();
        TypeSettingsGuiSettings gui = config.gui().typeSettings;
        Map<String, String> ph = typePlaceholders(definition);

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
        inventory.setItem(gui.despawnSlot, icon(
                Material.matchMaterial(gui.despawnMaterial),
                messages.resolve("gui.type-settings.despawn", ph),
                messages.resolveLore("gui.type-settings.despawn-lore", ph)
        ));
        inventory.setItem(gui.waveProfileSlot, icon(
                Material.matchMaterial(gui.waveProfileMaterial),
                messages.resolve("gui.type-settings.wave-profile", ph),
                messages.resolveLore("gui.type-settings.wave-profile-lore", ph)
        ));
        inventory.setItem(gui.lootInfoSlot, icon(
                Material.matchMaterial(gui.lootInfoMaterial),
                messages.resolve("gui.type-settings.loot", ph),
                messages.resolveLore("gui.type-settings.loot-lore", ph)
        ));
    }

    private Map<String, String> typePlaceholders(HordeTypeDefinition definition) {
        Map<String, String> values = new HashMap<>();
        values.put("type", typeId);
        values.put("world", definition.settings().worldPlacement.spawnWorld);
        values.put("profile", definition.settings().waveProfileId);
        values.put("loot", definition.lootTableId());
        values.put("count", Integer.toString(SerializedItemStackCodec.countEncodedEntries(
                definition.loot().poolItemsBase64
        )));
        values.put("occupied", Integer.toString(definition.loot().occupiedSlots));
        values.put("active", Integer.toString(service.countActive(typeId)));
        values.put("rolls_min", Integer.toString(definition.settings().mobLoot.rollsPerKillMin));
        values.put("rolls_max", Integer.toString(definition.settings().mobLoot.rollsPerKillMax));
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
