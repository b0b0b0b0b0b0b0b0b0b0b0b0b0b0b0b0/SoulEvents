package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;
import org.bukkit.entity.Player;

public final class MobWavesGuiFactory {

    private final MobWavesPlugin plugin;
    private MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobHordeService hordeService;

    public MobWavesGuiFactory(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobHordeService hordeService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.hordeService = hordeService;
    }

    public void reload(MobWavesPluginConfig config) {
        this.config = config;
    }

    public void openAdmin(Player player) {
        new MobWavesAdminMenu(config, messages, this).open(player);
    }

    public void openProfilesHub(Player player) {
        new MobWaveProfilesMenu(config, messages, this).open(player);
    }

    public void openTypeSettings(Player player, String typeId) {
        new MobHordeTypeSettingsMenu(config, messages, hordeService, this, typeId).open(player);
    }

    public void openLootHub(Player player, String typeId) {
        new MobHordeLootHubMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openObfuscationItems(Player player, String typeId) {
        new MobHordeObfuscationItemsMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openLootPool(Player player, String typeId, int page) {
        new MobHordeLootPoolMenu(plugin, config, messages, this, typeId, page).open(player);
    }

    public void openProfile(Player player, String profileId) {
        openProfile(player, profileId, 0);
    }

    public void openProfile(Player player, String profileId, int page) {
        new MobWaveProfileMenu(plugin, config, messages, this, profileId, page).open(player);
    }

    public void openWaveEditor(Player player, String profileId, int waveIndex) {
        new MobWaveEditorMenu(plugin, config, messages, this, profileId, waveIndex).open(player);
    }

    public void openMobSettings(Player player, String profileId) {
        new MobWaveMobSettingsMenu(config, messages, this, profileId).open(player);
    }

    public void openWaveSettings(Player player, String profileId, int waveIndex) {
        new MobWaveWaveSettingsMenu(plugin, config, messages, this, profileId, waveIndex).open(player);
    }

    public void openProfileSettings(Player player, String profileId) {
        new MobWaveProfileSettingsMenu(plugin, config, messages, this, profileId).open(player);
    }

    public void openMobEffects(Player player, String profileId, String mobType) {
        new MobWaveMobEffectsMenu(plugin, config, messages, this, profileId, mobType).open(player);
    }

    public void openMobOverride(Player player, String profileId, String mobType) {
        new MobWaveMobOverrideMenu(plugin, config, messages, this, profileId, mobType).open(player);
    }
}
