package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.settings.AdminHubGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.TypeListGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
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

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobWavesAdminMenu implements InventoryHolder {

    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final Inventory inventory;
    private final Map<Integer, String> slotTypes = new HashMap<>();

    public MobWavesAdminMenu(MobWavesPluginConfig config, MobWaveMessageService messages, MobWavesGuiFactory guiFactory) {
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
        if (event.getClickedInventory() != inventory || event.getRawSlot() != event.getSlot()) {
            return;
        }
        AdminHubGuiSettings hub = config.gui().adminHub;
        int slot = event.getRawSlot();
        if (slot == hub.waveProfilesSlot) {
            guiFactory.openProfilesHub(player);
            return;
        }
        String typeId = slotTypes.get(slot);
        if (typeId != null) {
            guiFactory.openTypeSettings(player, typeId);
        }
    }

    private void render() {
        inventory.clear();
        slotTypes.clear();
        AdminHubGuiSettings hub = config.gui().adminHub;
        TypeListGuiSettings list = config.gui().typeList;
        inventory.setItem(hub.waveProfilesSlot, icon(
                Material.matchMaterial(hub.waveProfilesMaterial),
                messages.resolve("gui.admin.wave-profiles", Map.of()),
                messages.resolveLore("gui.admin.wave-profiles-lore", Map.of(
                        "count", Integer.toString(config.profiles().size())
                ))
        ));
        int slot = hub.typeListStartSlot;
        List<HordeTypeDefinition> sortedTypes = config.types().stream()
                .sorted(Comparator.comparing(HordeTypeDefinition::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (HordeTypeDefinition definition : sortedTypes) {
            if (slot >= inventory.getSize()) {
                break;
            }
            Material material = definition.settings().enabled
                    ? Material.matchMaterial(list.defaultIconMaterial)
                    : Material.matchMaterial(list.disabledIconMaterial);
            List<Component> lore = messages.resolveLore("gui.admin.type-lore", Map.of(
                    "interval", Long.toString(definition.settings().intervalMinutes),
                    "world", definition.settings().worldPlacement.spawnWorld,
                    "profile", definition.settings().waveProfileId,
                    "loot", definition.lootTableId(),
                    "type", definition.id(),
                    "state", messages.resolvePlain(
                            definition.settings().enabled ? "gui.admin.type-enabled" : "gui.admin.type-disabled",
                            Map.of()
                    )
            ));
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
        ItemStack stack = new ItemStack(material == null ? Material.SPAWNER : material);
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
