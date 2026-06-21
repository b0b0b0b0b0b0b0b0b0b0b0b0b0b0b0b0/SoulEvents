package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.ProfileHubGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
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
    private final int page;
    private final int slotsPerPage;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotWaveIndex = new HashMap<>();

    public MobWaveProfileMenu(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String profileId,
            int page
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.profileId = profileId;
        ProfileHubGuiSettings gui = config.gui().profileHub;
        int waveCount = config.profile(profileId).map(profile -> profile.settings().waves.size()).orElse(0);
        this.slotsPerPage = waveListCapacity(gui);
        int pageCount = pageCount(waveCount, slotsPerPage);
        this.page = Math.max(0, Math.min(pageCount - 1, page));
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.profile.title", Map.of(
                        "profile", profileId,
                        "page", Integer.toString(this.page + 1),
                        "pages", Integer.toString(pageCount)
                ))
        );
        render();
    }

    public String profileId() {
        return profileId;
    }

    public int page() {
        return page;
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
        if (slot == gui.prevPageSlot && page > 0) {
            guiFactory.openProfile(player, profileId, page - 1);
            return;
        }
        if (slot == gui.nextPageSlot) {
            int pageCount = pageCount(currentWaveCount(), slotsPerPage);
            if (page + 1 < pageCount) {
                guiFactory.openProfile(player, profileId, page + 1);
            }
            return;
        }
        if (slot == gui.infoSlot || slot == gui.pageInfoSlot) {
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
        int newIndex = profile.settings().waves.size() - 1;
        int targetPage = newIndex / slotsPerPage;
        guiFactory.openProfile(player, profileId, targetPage);
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        slotWaveIndex.clear();
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        ProfileHubGuiSettings gui = config.gui().profileHub;
        List<WaveDefinitionSettings> waves = profile.settings().waves;
        int pageCount = pageCount(waves.size(), slotsPerPage);
        Map<String, String> pagePlaceholders = Map.of(
                "profile", profileId,
                "page", Integer.toString(page + 1),
                "pages", Integer.toString(pageCount),
                "waves", Integer.toString(waves.size())
        );
        inventory.setItem(gui.infoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.profile.info", pagePlaceholders),
                messages.resolveLore("gui.profile.info-lore", pagePlaceholders)
        ));
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.profile.back", Map.of()),
                messages.resolveLore("gui.profile.back-lore", Map.of())
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
        if (page > 0) {
            inventory.setItem(gui.prevPageSlot, GuiIcons.icon(
                    Material.matchMaterial(gui.prevPageMaterial),
                    messages.resolve("gui.profile.prev-page", pagePlaceholders),
                    messages.resolveLore("gui.profile.prev-page-lore", pagePlaceholders)
            ));
        }
        inventory.setItem(gui.pageInfoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.pageInfoMaterial),
                messages.resolve("gui.profile.page-info", pagePlaceholders),
                messages.resolveLore("gui.profile.page-info-lore", pagePlaceholders)
        ));
        if (page + 1 < pageCount) {
            inventory.setItem(gui.nextPageSlot, GuiIcons.icon(
                    Material.matchMaterial(gui.nextPageMaterial),
                    messages.resolve("gui.profile.next-page", pagePlaceholders),
                    messages.resolveLore("gui.profile.next-page-lore", pagePlaceholders)
            ));
        }
        int pageStart = page * slotsPerPage;
        for (int offset = 0; offset < slotsPerPage; offset++) {
            int waveIndex = pageStart + offset;
            if (waveIndex >= waves.size()) {
                break;
            }
            int slot = gui.waveListStartSlot + offset;
            if (slot > gui.waveListEndSlot) {
                break;
            }
            WaveDefinitionSettings wave = waves.get(waveIndex);
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
            slotWaveIndex.put(slot, waveIndex);
        }
    }

    private int currentWaveCount() {
        return config.profile(profileId).map(profile -> profile.settings().waves.size()).orElse(0);
    }

    private static int waveListCapacity(ProfileHubGuiSettings gui) {
        return Math.max(1, gui.waveListEndSlot - gui.waveListStartSlot + 1);
    }

    private static int pageCount(int waveCount, int slotsPerPage) {
        if (waveCount <= 0) {
            return 1;
        }
        return (waveCount + slotsPerPage - 1) / slotsPerPage;
    }
}
