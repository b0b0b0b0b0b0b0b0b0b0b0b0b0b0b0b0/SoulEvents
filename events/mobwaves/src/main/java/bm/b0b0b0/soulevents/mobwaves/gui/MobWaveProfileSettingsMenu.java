package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveProfileSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveProfileSettingsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public final class MobWaveProfileSettingsMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final Inventory inventory;

    public MobWaveProfileSettingsMenu(
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
        WaveProfileSettingsGuiSettings gui = config.gui().waveProfileSettings;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.profile-settings.title", Map.of("profile", profileId))
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
        Optional<WaveProfileSettings> settingsOptional = currentSettings();
        if (settingsOptional.isEmpty()) {
            return;
        }
        WaveProfileSettings settings = settingsOptional.get();
        WaveProfileSettingsGuiSettings gui = config.gui().waveProfileSettings;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openProfile(player, profileId);
            return;
        }
        if (slot == gui.infoSlot) {
            return;
        }
        if (slot == gui.spawnRadiusMinusSlot) {
            settings.spawnRadius = Math.max(0, settings.spawnRadius - 4);
        } else if (slot == gui.spawnRadiusPlusSlot) {
            settings.spawnRadius = Math.min(128, settings.spawnRadius + 4);
        } else if (slot == gui.batchSizeMinusSlot) {
            settings.batchSize = Math.max(0, settings.batchSize <= 0 ? 3 : settings.batchSize - 1);
        } else if (slot == gui.batchSizePlusSlot) {
            settings.batchSize = Math.min(32, settings.batchSize <= 0 ? 5 : settings.batchSize + 1);
        } else if (slot == gui.intervalMinusSlot) {
            settings.batchIntervalTicks = Math.max(0, settings.batchIntervalTicks <= 0 ? 15 : settings.batchIntervalTicks - 5);
        } else if (slot == gui.intervalPlusSlot) {
            settings.batchIntervalTicks = Math.min(200, settings.batchIntervalTicks <= 0 ? 25 : settings.batchIntervalTicks + 5);
        } else if (slot == gui.graceMinusSlot) {
            settings.graceAfterClearSeconds = Math.max(0, settings.graceAfterClearSeconds <= 0 ? 8 : settings.graceAfterClearSeconds - 2);
        } else if (slot == gui.gracePlusSlot) {
            settings.graceAfterClearSeconds = Math.min(120, settings.graceAfterClearSeconds <= 0 ? 12 : settings.graceAfterClearSeconds + 2);
        } else {
            return;
        }
        save(settings);
        render();
    }

    private Optional<WaveProfileSettings> currentSettings() {
        return config.profile(profileId).map(WaveProfileDefinition::settings);
    }

    private void save(WaveProfileSettings settings) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        ProfileDirectoryLoader.save(plugin, profileOptional.get());
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        WaveProfileSettings settings = currentSettings().orElse(new WaveProfileSettings());
        WaveProfileSettingsGuiSettings gui = config.gui().waveProfileSettings;
        Map<String, String> ph = Map.of(
                "profile", profileId,
                "spawn", displayValue(settings.spawnRadius),
                "batch", displayValue(settings.batchSize),
                "interval", displayValue(settings.batchIntervalTicks),
                "grace", displayValue(settings.graceAfterClearSeconds)
        );
        inventory.setItem(gui.infoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.profile-settings.info", ph),
                messages.resolveLore("gui.profile-settings.info-lore", ph)
        ));
        putRow(gui.spawnRadiusMinusSlot, gui.spawnRadiusInfoSlot, gui.spawnRadiusPlusSlot,
                Material.COMPASS, "spawn-radius", ph);
        putRow(gui.batchSizeMinusSlot, gui.batchSizeInfoSlot, gui.batchSizePlusSlot,
                Material.IRON_SWORD, "batch-size", ph);
        putRow(gui.intervalMinusSlot, gui.intervalInfoSlot, gui.intervalPlusSlot,
                Material.CLOCK, "interval", ph);
        putRow(gui.graceMinusSlot, gui.graceInfoSlot, gui.gracePlusSlot,
                Material.CHEST, "grace", ph);
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.profile-settings.back", ph),
                messages.resolveLore("gui.profile-settings.back-lore", ph)
        ));
    }

    private void putRow(int minusSlot, int infoSlot, int plusSlot, Material icon, String key, Map<String, String> ph) {
        inventory.setItem(minusSlot, GuiIcons.icon(
                Material.RED_DYE,
                messages.resolve("gui.profile-settings." + key + "-minus", ph),
                messages.resolveLore("gui.profile-settings." + key + "-minus-lore", ph)
        ));
        inventory.setItem(infoSlot, GuiIcons.icon(
                icon,
                messages.resolve("gui.profile-settings." + key, ph),
                messages.resolveLore("gui.profile-settings." + key + "-lore", ph)
        ));
        inventory.setItem(plusSlot, GuiIcons.icon(
                Material.LIME_DYE,
                messages.resolve("gui.profile-settings." + key + "-plus", ph),
                messages.resolveLore("gui.profile-settings." + key + "-plus-lore", ph)
        ));
    }

    private String displayValue(int value) {
        return value > 0 ? Integer.toString(value) : messages.resolvePlain("gui.common.default-value", Map.of());
    }
}
