package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.ProfileHubGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveProfileMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotWaveIndex = new HashMap<>();

    public MobWaveProfileMenu(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String profileId
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.profileId = profileId;
        ProfileHubGuiSettings gui = config.gui().profileHub;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.profile.title", Map.of("profile", profileId))
        );
        render();
    }

    public String profileId() {
        return profileId;
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
        ProfileHubGuiSettings gui = config.gui().profileHub;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openProfilesHub(player);
            return;
        }
        if (slot == gui.addWaveSlot) {
            addWave(player);
            return;
        }
        if (slot == gui.mobSettingsSlot) {
            guiFactory.openMobSettings(player, profileId);
            return;
        }
        if (slot == gui.profileSettingsSlot) {
            guiFactory.openProfileSettings(player, profileId);
            return;
        }
        Integer waveIndex = slotWaveIndex.get(slot);
        if (waveIndex != null) {
            if (event.getClick().isRightClick()) {
                guiFactory.openWaveSettings(player, profileId, waveIndex);
            } else {
                guiFactory.openWaveEditor(player, profileId, waveIndex);
            }
        }
    }

    private void addWave(Player player) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        WaveDefinitionSettings wave = new WaveDefinitionSettings();
        wave.name = "Wave " + (profile.settings().waves.size() + 1);
        profile.settings().waves.add(wave);
        ProfileDirectoryLoader.save(plugin, profile);
        guiFactory.openProfile(player, profileId);
    }

    private void render() {
        inventory.clear();
        slotWaveIndex.clear();
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        ProfileHubGuiSettings gui = config.gui().profileHub;
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.profile.back", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.addWaveSlot, GuiIcons.icon(
                Material.matchMaterial(gui.addWaveMaterial),
                messages.resolve("gui.profile.add-wave", Map.of()),
                messages.resolveLore("gui.profile.add-wave-lore", Map.of())
        ));
        inventory.setItem(gui.mobSettingsSlot, GuiIcons.icon(
                Material.matchMaterial(gui.mobSettingsMaterial),
                messages.resolve("gui.profile.mob-settings", Map.of()),
                messages.resolveLore("gui.profile.mob-settings-lore", Map.of())
        ));
        inventory.setItem(gui.profileSettingsSlot, GuiIcons.icon(
                Material.matchMaterial(gui.profileSettingsMaterial),
                messages.resolve("gui.profile.profile-settings", Map.of()),
                messages.resolveLore("gui.profile.profile-settings-lore", Map.of())
        ));
        int slot = gui.waveListStartSlot;
        for (int index = 0; index < profile.settings().waves.size(); index++) {
            if (slot >= inventory.getSize()) {
                break;
            }
            WaveDefinitionSettings wave = profile.settings().waves.get(index);
            int mobCount = wave.entries.stream().mapToInt(entry -> Math.max(0, entry.count)).sum();
            String bossState = wave.superBossEnabled ? "on" : "off";
            inventory.setItem(slot, GuiIcons.icon(
                    Material.PAPER,
                    messages.resolve("gui.profile.wave-item", Map.of("name", wave.name)),
                    messages.resolveLore("gui.profile.wave-lore", Map.of(
                            "count", Integer.toString(mobCount),
                            "boss", bossState
                    ))
            ));
            slotWaveIndex.put(slot, index);
            slot++;
        }
    }
}
