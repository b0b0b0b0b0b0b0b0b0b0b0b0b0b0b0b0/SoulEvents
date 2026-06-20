package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.ProfilesHubGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MobWaveProfilesMenu implements InventoryHolder {

    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final Inventory inventory;
    private final Map<Integer, String> slotProfiles = new HashMap<>();

    public MobWaveProfilesMenu(MobWavesPluginConfig config, MobWaveMessageService messages, MobWavesGuiFactory guiFactory) {
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        ProfilesHubGuiSettings gui = config.gui().profilesHub;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.profiles-hub.title", Map.of())
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
        if (event.getClickedInventory() != inventory || event.getRawSlot() != event.getSlot()) {
            return;
        }
        ProfilesHubGuiSettings gui = config.gui().profilesHub;
        if (event.getRawSlot() == gui.backSlot) {
            guiFactory.openAdmin(player);
            return;
        }
        String profileId = slotProfiles.get(event.getRawSlot());
        if (profileId != null) {
            guiFactory.openProfile(player, profileId);
        }
    }

    private void render() {
        inventory.clear();
        slotProfiles.clear();
        ProfilesHubGuiSettings gui = config.gui().profilesHub;
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.profiles-hub.back", Map.of()),
                messages.resolveLore("gui.profiles-hub.back-lore", Map.of())
        ));
        int slot = gui.profileListStartSlot;
        List<WaveProfileDefinition> profiles = config.profiles().stream()
                .sorted(Comparator.comparing(WaveProfileDefinition::id, String.CASE_INSENSITIVE_ORDER))
                .toList();
        for (WaveProfileDefinition profile : profiles) {
            if (slot >= inventory.getSize()) {
                break;
            }
            Material material = Material.matchMaterial(gui.defaultProfileIconMaterial);
            Component name = Component.text(profile.id());
            List<Component> lore = messages.resolveLore("gui.admin.profile-lore", Map.of(
                    "waves", Integer.toString(profile.settings().waves.size())
            ));
            inventory.setItem(slot, GuiIcons.icon(material, name, lore));
            slotProfiles.put(slot, profile.id());
            slot++;
        }
    }
}
