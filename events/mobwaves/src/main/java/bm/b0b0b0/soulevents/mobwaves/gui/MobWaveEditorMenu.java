package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveDefinitionSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveEditorGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.config.settings.WaveMobEntrySettings;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import org.bukkit.entity.EntityType;
import bm.b0b0b0.soulevents.mobwaves.util.WaveEntryCodec;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveEditorMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final int waveIndex;
    private final Inventory inventory;
    private final int editableSlotCount;
    private boolean skipCloseSave;

    public MobWaveEditorMenu(
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
        WaveEditorGuiSettings gui = config.gui().waveEditor;
        this.editableSlotCount = Math.min(gui.rows * 9, Math.max(1, gui.editableSlotCount));
        WaveDefinitionSettings wave = resolveWave().orElse(new WaveDefinitionSettings());
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.wave-editor.title", Map.of(
                        "index", Integer.toString(waveIndex + 1),
                        "name", wave.name
                ))
        );
        renderFrame();
        WaveEntryCodec.writeInventory(inventory, editableSlotCount, wave.entries);
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

    public boolean isEditableSlot(int rawSlot) {
        return rawSlot >= 0 && rawSlot < editableSlotCount && !isFrameSlot(rawSlot);
    }

    private boolean isFrameSlot(int rawSlot) {
        WaveEditorGuiSettings gui = config.gui().waveEditor;
        return rawSlot == gui.backSlot
                || rawSlot == gui.saveSlot
                || rawSlot == gui.waveSettingsSlot
                || rawSlot == gui.bossSlot
                || rawSlot == gui.deleteWaveSlot;
    }

    public void handleClick(InventoryClickEvent event) {
        WaveEditorGuiSettings gui = config.gui().waveEditor;
        int rawSlot = event.getRawSlot();
        if (rawSlot == gui.waveSettingsSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persist(playerFrom(event));
            guiFactory.openWaveSettings(playerFrom(event), profileId, waveIndex);
            return;
        }
        if (rawSlot == gui.deleteWaveSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            deleteWave(playerFrom(event));
            return;
        }
        if (rawSlot == gui.bossSlot) {
            event.setCancelled(true);
            cycleBossType();
            renderFrame();
            return;
        }
        if (rawSlot == gui.backSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persist(playerFrom(event));
            guiFactory.openProfile(playerFrom(event), profileId);
            return;
        }
        if (rawSlot == gui.saveSlot) {
            event.setCancelled(true);
            skipCloseSave = true;
            persist(playerFrom(event));
            messages.send(playerFrom(event), "mobwaves.wave-saved", Map.of());
            guiFactory.openProfile(playerFrom(event), profileId);
            return;
        }
        if (event.getClickedInventory() == inventory && isEditableSlot(rawSlot)) {
            if (event.getCursor() != null && !event.getCursor().getType().isAir()) {
                if (MobWaveEntitySupport.fromSpawnEgg(event.getCursor().getType()).isEmpty()) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (event.getClickedInventory() == inventory || event.getView().getTopInventory() == inventory) {
            event.setCancelled(true);
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < inventory.getSize() && !isEditableSlot(rawSlot)) {
                event.setCancelled(true);
                return;
            }
        }
        ItemStack cursor = event.getOldCursor();
        if (cursor != null && !cursor.getType().isAir()
                && MobWaveEntitySupport.fromSpawnEgg(cursor.getType()).isEmpty()) {
            event.setCancelled(true);
        }
    }

    public void handleClose(InventoryCloseEvent event) {
        if (skipCloseSave) {
            return;
        }
        persist((Player) event.getPlayer());
    }

    private Player playerFrom(InventoryClickEvent event) {
        return (Player) event.getWhoClicked();
    }

    private void persist(Player player) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty() || waveIndex < 0 || waveIndex >= profileOptional.get().settings().waves.size()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        profile.settings().waves.get(waveIndex).entries = WaveEntryCodec.readInventory(inventory, editableSlotCount);
        ProfileDirectoryLoader.save(plugin, profile);
    }

    private Optional<WaveDefinitionSettings> resolveWave() {
        return config.profile(profileId)
                .filter(profile -> waveIndex >= 0 && waveIndex < profile.settings().waves.size())
                .map(profile -> profile.settings().waves.get(waveIndex));
    }

    private void renderFrame() {
        WaveEditorGuiSettings gui = config.gui().waveEditor;
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.wave-editor.back", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.saveSlot, GuiIcons.icon(
                Material.matchMaterial(gui.saveMaterial),
                messages.resolve("gui.wave-editor.save", Map.of()),
                messages.resolveLore("gui.wave-editor.save-lore", Map.of())
        ));
        inventory.setItem(gui.waveSettingsSlot, GuiIcons.icon(
                Material.matchMaterial(gui.waveSettingsMaterial),
                messages.resolve("gui.wave-editor.wave-settings", Map.of()),
                messages.resolveLore("gui.wave-editor.wave-settings-lore", Map.of())
        ));
        WaveDefinitionSettings wave = resolveWave().orElse(new WaveDefinitionSettings());
        if (wave.superBoss == null) {
            wave.superBoss = WaveDefinitionSettings.defaultSuperBoss();
        }
        EntityType bossType = MobWaveEntitySupport.resolveEntityType(wave.superBoss.entityType);
        Material bossEgg = bossType == null ? Material.BARRIER : MobWaveEntitySupport.spawnEgg(bossType);
        inventory.setItem(gui.bossSlot, GuiIcons.icon(
                bossEgg,
                messages.resolve("gui.wave-editor.boss-slot", Map.of(
                        "enabled", wave.superBossEnabled ? "on" : "off",
                        "type", bossType == null ? wave.superBoss.entityType : bossType.name()
                )),
                messages.resolveLore("gui.wave-editor.boss-slot-lore", Map.of())
        ));
        inventory.setItem(gui.deleteWaveSlot, GuiIcons.icon(
                Material.matchMaterial(gui.deleteWaveMaterial),
                messages.resolve("gui.wave-editor.delete-wave", Map.of()),
                messages.resolveLore("gui.wave-editor.delete-wave-lore", Map.of())
        ));
    }

    private void deleteWave(Player player) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty() || waveIndex < 0 || waveIndex >= profileOptional.get().settings().waves.size()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        profile.settings().waves.remove(waveIndex);
        ProfileDirectoryLoader.save(plugin, profile);
        messages.send(player, "mobwaves.wave-deleted", Map.of());
        guiFactory.openProfile(player, profileId);
    }

    private void cycleBossType() {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty() || waveIndex < 0 || waveIndex >= profileOptional.get().settings().waves.size()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        WaveDefinitionSettings wave = profile.settings().waves.get(waveIndex);
        if (wave.superBoss == null) {
            wave.superBoss = WaveDefinitionSettings.defaultSuperBoss();
        }
        EntityType next = MobWaveEntitySupport.nextAllowedType(wave.superBoss.entityType);
        wave.superBoss.entityType = next.name();
        ProfileDirectoryLoader.save(plugin, profile);
    }
}
