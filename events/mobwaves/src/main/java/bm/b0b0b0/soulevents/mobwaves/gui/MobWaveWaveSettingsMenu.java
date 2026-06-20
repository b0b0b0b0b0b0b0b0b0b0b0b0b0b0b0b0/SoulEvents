package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveWaveSettingsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveWaveSettingsMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final int waveIndex;
    private final Inventory inventory;

    public MobWaveWaveSettingsMenu(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String profileId,
            int waveIndex
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.profileId = profileId;
        this.waveIndex = waveIndex;
        WaveWaveSettingsGuiSettings gui = config.gui().waveWaveSettings;
        WaveDefinitionSettings wave = resolveWave().orElse(new WaveDefinitionSettings());
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.wave-settings.title", Map.of(
                        "index", Integer.toString(waveIndex + 1),
                        "name", wave.name
                ))
        );
        render();
    }

    public String profileId() {
        return profileId;
    }

    public int waveIndex() {
        return waveIndex;
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
        Optional<WaveDefinitionSettings> waveOptional = resolveWave();
        if (waveOptional.isEmpty()) {
            return;
        }
        WaveDefinitionSettings wave = waveOptional.get();
        if (wave.superBoss == null) {
            wave.superBoss = WaveDefinitionSettings.defaultSuperBoss();
        }
        WaveWaveSettingsGuiSettings gui = config.gui().waveWaveSettings;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openProfile(player, profileId);
            return;
        }
        if (slot == gui.deleteWaveSlot) {
            deleteWave(player);
            return;
        }
        if (slot == gui.toggleBossSlot) {
            wave.superBossEnabled = !wave.superBossEnabled;
            save(wave);
            render();
            return;
        }
        if (slot == gui.bossTypeSlot) {
            EntityType next = MobWaveEntitySupport.nextAllowedType(wave.superBoss.entityType);
            wave.superBoss.entityType = next.name();
            save(wave);
            render();
            return;
        }
        if (slot == gui.bossHealthMinusSlot) {
            adjustBossHealth(wave, -25.0);
            save(wave);
            render();
            return;
        }
        if (slot == gui.bossHealthPlusSlot) {
            adjustBossHealth(wave, 25.0);
            save(wave);
            render();
        }
    }

    private void adjustBossHealth(WaveDefinitionSettings wave, double delta) {
        WaveMobEntrySettings boss = wave.superBoss;
        if (boss.maxHealth <= 0.0) {
            boss.maxHealth = 250.0;
        }
        boss.maxHealth = Math.max(0.0, Math.min(5000.0, boss.maxHealth + delta));
    }

    private void deleteWave(Player player) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        if (waveIndex < 0 || waveIndex >= profile.settings().waves.size()) {
            return;
        }
        profile.settings().waves.remove(waveIndex);
        ProfileDirectoryLoader.save(plugin, profile);
        messages.send(player, "mobwaves.wave-deleted", Map.of());
        guiFactory.openProfile(player, profileId);
    }

    private void save(WaveDefinitionSettings wave) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        if (waveIndex < 0 || waveIndex >= profile.settings().waves.size()) {
            return;
        }
        profile.settings().waves.set(waveIndex, wave);
        ProfileDirectoryLoader.save(plugin, profile);
    }

    private Optional<WaveDefinitionSettings> resolveWave() {
        return config.profile(profileId)
                .filter(profile -> waveIndex >= 0 && waveIndex < profile.settings().waves.size())
                .map(profile -> profile.settings().waves.get(waveIndex));
    }

    private void render() {
        inventory.clear();
        WaveDefinitionSettings wave = resolveWave().orElse(new WaveDefinitionSettings());
        if (wave.superBoss == null) {
            wave.superBoss = WaveDefinitionSettings.defaultSuperBoss();
        }
        WaveWaveSettingsGuiSettings gui = config.gui().waveWaveSettings;
        String bossHealth = wave.superBoss.maxHealth > 0.0
                ? Integer.toString((int) wave.superBoss.maxHealth)
                : "default";
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.wave-settings.back", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.toggleBossSlot, GuiIcons.icon(
                Material.matchMaterial(gui.toggleBossMaterial),
                messages.resolve("gui.wave-settings.boss-toggle", Map.of(
                        "state", wave.superBossEnabled ? "on" : "off"
                )),
                messages.resolveLore("gui.wave-settings.boss-toggle-lore", Map.of())
        ));
        EntityType bossType = MobWaveEntitySupport.resolveEntityType(wave.superBoss.entityType);
        Material egg = bossType == null ? Material.BARRIER : MobWaveEntitySupport.spawnEgg(bossType);
        inventory.setItem(gui.bossTypeSlot, GuiIcons.icon(
                egg,
                messages.resolve("gui.wave-settings.boss-type", Map.of(
                        "type", bossType == null ? wave.superBoss.entityType : bossType.name()
                )),
                messages.resolveLore("gui.wave-settings.boss-type-lore", Map.of())
        ));
        inventory.setItem(gui.bossHealthMinusSlot, GuiIcons.icon(
                Material.RED_DYE,
                messages.resolve("gui.wave-settings.boss-health-minus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.bossHealthInfoSlot, GuiIcons.icon(
                Material.GOLDEN_APPLE,
                messages.resolve("gui.wave-settings.boss-health-info", Map.of("health", bossHealth)),
                messages.resolveLore("gui.wave-settings.boss-health-lore", Map.of())
        ));
        inventory.setItem(gui.bossHealthPlusSlot, GuiIcons.icon(
                Material.LIME_DYE,
                messages.resolve("gui.wave-settings.boss-health-plus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.deleteWaveSlot, GuiIcons.icon(
                Material.matchMaterial(gui.deleteWaveMaterial),
                messages.resolve("gui.wave-settings.delete-wave", Map.of()),
                messages.resolveLore("gui.wave-settings.delete-wave-lore", Map.of())
        ));
    }
}
