package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobOverrideGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveMobOverrideMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final String mobType;
    private final Inventory inventory;

    public MobWaveMobOverrideMenu(
            MobWavesPlugin plugin,
            MobWavesPluginConfig config,
            MobWaveMessageService messages,
            MobWavesGuiFactory guiFactory,
            String profileId,
            String mobType
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        this.profileId = profileId;
        this.mobType = mobType;
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        this.inventory = Bukkit.createInventory(
                this,
                gui.rows * 9,
                messages.resolve("gui.mob-override.title", Map.of("mob", mobType))
        );
        render();
    }

    public String profileId() {
        return profileId;
    }

    public String mobType() {
        return mobType;
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
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openMobSettings(player, profileId);
            return;
        }
        Optional<MobTypeOverrideSettings> overrideOptional = currentOverride();
        if (overrideOptional.isEmpty()) {
            return;
        }
        MobTypeOverrideSettings override = overrideOptional.get();
        if (slot == gui.healthMinusSlot) {
            override.maxHealth = Math.max(0.0, override.maxHealth <= 0.0 ? 250.0 : override.maxHealth - 25.0);
        } else if (slot == gui.healthPlusSlot) {
            override.maxHealth = Math.min(5000.0, (override.maxHealth <= 0.0 ? 250.0 : override.maxHealth) + 25.0);
        } else if (slot == gui.speedMinusSlot) {
            override.speedMultiplier = Math.max(0.1, override.speedMultiplier - 0.1);
        } else if (slot == gui.speedPlusSlot) {
            override.speedMultiplier = Math.min(3.0, override.speedMultiplier + 0.1);
        } else if (slot == gui.damageMinusSlot) {
            override.damageMultiplier = Math.max(0.1, override.damageMultiplier - 0.1);
        } else if (slot == gui.damagePlusSlot) {
            override.damageMultiplier = Math.min(3.0, override.damageMultiplier + 0.1);
        } else {
            return;
        }
        save(override);
        render();
    }

    private Optional<MobTypeOverrideSettings> currentOverride() {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(profileOptional.get().settings().mobOverrides.computeIfAbsent(
                mobType,
                ignored -> new MobTypeOverrideSettings()
        ));
    }

    private void save(MobTypeOverrideSettings override) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        profile.settings().mobOverrides.put(mobType, override);
        ProfileDirectoryLoader.save(plugin, profile);
    }

    private void render() {
        inventory.clear();
        MobTypeOverrideSettings override = currentOverride().orElse(new MobTypeOverrideSettings());
        String health = override.maxHealth > 0.0
                ? Integer.toString((int) override.maxHealth)
                : "default";
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.mob-override.back", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.healthMinusSlot, GuiIcons.icon(
                Material.RED_DYE,
                messages.resolve("gui.mob-override.health-minus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.healthInfoSlot, GuiIcons.icon(
                Material.APPLE,
                messages.resolve("gui.mob-override.health-info", Map.of("health", health)),
                List.of()
        ));
        inventory.setItem(gui.healthPlusSlot, GuiIcons.icon(
                Material.LIME_DYE,
                messages.resolve("gui.mob-override.health-plus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.speedMinusSlot, GuiIcons.icon(
                Material.FEATHER,
                messages.resolve("gui.mob-override.speed-minus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.speedPlusSlot, GuiIcons.icon(
                Material.SUGAR,
                messages.resolve("gui.mob-override.speed-plus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.damageMinusSlot, GuiIcons.icon(
                Material.IRON_SWORD,
                messages.resolve("gui.mob-override.damage-minus", Map.of()),
                List.of()
        ));
        inventory.setItem(gui.damagePlusSlot, GuiIcons.icon(
                Material.DIAMOND_SWORD,
                messages.resolve("gui.mob-override.damage-plus", Map.of()),
                List.of()
        ));
    }
}
