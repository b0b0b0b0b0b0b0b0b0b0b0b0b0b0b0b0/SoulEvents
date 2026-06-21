package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobSettingsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityNames;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveMobSettingsMenu implements InventoryHolder {

    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final Inventory inventory;
    private final Map<Integer, String> slotTypes = new HashMap<>();

    public MobWaveMobSettingsMenu(
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String profileId
    ) {
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.profileId = profileId;
        MobSettingsGuiSettings gui = config.gui().mobSettings;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.mob-settings.title", Map.of("profile", profileId))
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
        MobSettingsGuiSettings gui = config.gui().mobSettings;
        if (event.getRawSlot() == gui.backSlot) {
            guiFactory.openProfile(player, profileId);
            return;
        }
        if (event.getRawSlot() == gui.infoSlot) {
            return;
        }
        String mobType = slotTypes.get(event.getRawSlot());
        if (mobType != null) {
            guiFactory.openMobOverride(player, profileId, mobType);
        }
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        slotTypes.clear();
        MobSettingsGuiSettings gui = config.gui().mobSettings;
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        Map<String, String> ph = Map.of("profile", profileId);
        inventory.setItem(gui.infoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.mob-settings.info", ph),
                messages.resolveLore("gui.mob-settings.info-lore", ph)
        ));
        String defaultLabel = messages.resolvePlain("gui.common.default-value", Map.of());
        int slot = gui.mobListStartSlot;
        int maxSlot = Math.min(gui.mobListEndSlot, gui.backSlot - 1);
        for (EntityType type : MobWaveEntitySupport.allowedTypes()) {
            if (slot > maxSlot) {
                break;
            }
            MobTypeOverrideSettings override = profile.settings().mobOverrides.getOrDefault(
                    type.name(),
                    new MobTypeOverrideSettings()
            );
            int effectCount = override.effects == null ? 0 : override.effects.size();
            String health = override.maxHealth > 0.0
                    ? Integer.toString((int) override.maxHealth)
                    : defaultLabel;
            Map<String, String> entryPh = Map.of(
                    "health", health,
                    "speed", formatMultiplier(override.speedMultiplier),
                    "damage", formatMultiplier(override.damageMultiplier),
                    "effects", Integer.toString(effectCount)
            );
            ItemStack egg = new ItemStack(MobWaveEntitySupport.spawnEgg(type));
            egg.editMeta(meta -> {
                meta.displayName(MobWaveEntityNames.displayName(type));
                meta.lore(messages.resolveLore("gui.mob-settings.entry-lore", entryPh));
            });
            inventory.setItem(slot, egg);
            slotTypes.put(slot, type.name());
            slot++;
        }
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.mob-settings.back", ph),
                messages.resolveLore("gui.mob-settings.back-lore", ph)
        ));
    }

    private static String formatMultiplier(double value) {
        return value <= 0.0 ? "1.0" : String.format("%.1f", value);
    }
}
