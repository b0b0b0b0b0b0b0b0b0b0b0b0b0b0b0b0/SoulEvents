package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobOverrideGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.service.HordeMobCombatApplier;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityNames;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public final class MobWaveMobOverrideMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final EntityType entityType;
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
        this.entityType = MobWaveEntitySupport.resolveEntityType(mobType);
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        Component title = messages.resolve("gui.mob-override.title-prefix", Map.of())
                .append(entityType == null ? Component.text(mobType) : MobWaveEntityNames.displayName(entityType));
        this.inventory = Bukkit.createInventory(this, gui.rows * 9, title);
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
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot || slot == gui.infoSlot) {
            if (slot == gui.backSlot) {
                guiFactory.openMobSettings(player, profileId);
            }
            return;
        }
        if (slot == gui.effectsSlot) {
            if (entityType != null) {
                guiFactory.openMobEffects(player, profileId, entityType.name());
            }
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
            override.maxHealth = Math.min(HordeMobCombatApplier.MAX_LIVING_HEALTH, (override.maxHealth <= 0.0 ? 250.0 : override.maxHealth) + 25.0);
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
        if (profileOptional.isEmpty() || entityType == null) {
            return Optional.empty();
        }
        return Optional.of(profileOptional.get().settings().mobOverrides.computeIfAbsent(
                entityType.name(),
                ignored -> new MobTypeOverrideSettings()
        ));
    }

    private void save(MobTypeOverrideSettings override) {
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty() || entityType == null) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        profile.settings().mobOverrides.put(entityType.name(), override);
        ProfileDirectoryLoader.save(plugin, profile);
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        MobTypeOverrideSettings override = currentOverride().orElse(new MobTypeOverrideSettings());
        if (override.effects == null) {
            override.effects = new java.util.ArrayList<>();
        }
        MobOverrideGuiSettings gui = config.gui().mobOverride;
        Map<String, String> ph = Map.of(
                "profile", profileId,
                "health", formatHealth(override.maxHealth, messages),
                "speed", formatMultiplier(override.speedMultiplier),
                "damage", formatMultiplier(override.damageMultiplier),
                "effects", Integer.toString(override.effects.size())
        );
        inventory.setItem(gui.infoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.mob-override.info", ph),
                messages.resolveLore("gui.mob-override.info-lore", ph)
        ));
        inventory.setItem(gui.healthMinusSlot, GuiIcons.icon(
                Material.RED_DYE,
                messages.resolve("gui.mob-override.health-minus", ph),
                messages.resolveLore("gui.mob-override.health-minus-lore", ph)
        ));
        inventory.setItem(gui.healthInfoSlot, GuiIcons.icon(
                Material.APPLE,
                messages.resolve("gui.mob-override.health-info", ph),
                messages.resolveLore("gui.mob-override.health-info-lore", ph)
        ));
        inventory.setItem(gui.healthPlusSlot, GuiIcons.icon(
                Material.LIME_DYE,
                messages.resolve("gui.mob-override.health-plus", ph),
                messages.resolveLore("gui.mob-override.health-plus-lore", ph)
        ));
        inventory.setItem(gui.speedMinusSlot, GuiIcons.icon(
                Material.FEATHER,
                messages.resolve("gui.mob-override.speed-minus", ph),
                messages.resolveLore("gui.mob-override.speed-minus-lore", ph)
        ));
        inventory.setItem(gui.speedInfoSlot, GuiIcons.icon(
                Material.SUGAR,
                messages.resolve("gui.mob-override.speed-info", ph),
                messages.resolveLore("gui.mob-override.speed-info-lore", ph)
        ));
        inventory.setItem(gui.speedPlusSlot, GuiIcons.icon(
                Material.SUGAR,
                messages.resolve("gui.mob-override.speed-plus", ph),
                messages.resolveLore("gui.mob-override.speed-plus-lore", ph)
        ));
        inventory.setItem(gui.damageMinusSlot, GuiIcons.icon(
                Material.IRON_SWORD,
                messages.resolve("gui.mob-override.damage-minus", ph),
                messages.resolveLore("gui.mob-override.damage-minus-lore", ph)
        ));
        inventory.setItem(gui.damageInfoSlot, GuiIcons.icon(
                Material.DIAMOND_SWORD,
                messages.resolve("gui.mob-override.damage-info", ph),
                messages.resolveLore("gui.mob-override.damage-info-lore", ph)
        ));
        inventory.setItem(gui.damagePlusSlot, GuiIcons.icon(
                Material.NETHERITE_SWORD,
                messages.resolve("gui.mob-override.damage-plus", ph),
                messages.resolveLore("gui.mob-override.damage-plus-lore", ph)
        ));
        inventory.setItem(gui.effectsSlot, GuiIcons.icon(
                Material.matchMaterial(gui.effectsMaterial),
                messages.resolve("gui.mob-override.effects", ph),
                messages.resolveLore("gui.mob-override.effects-lore", ph)
        ));
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.mob-override.back", ph),
                messages.resolveLore("gui.mob-override.back-lore", ph)
        ));
    }

    private static String formatHealth(double value, MobWaveMessageService messages) {
        return value > 0.0 ? Integer.toString((int) value) : messages.resolvePlain("gui.common.default-value", Map.of());
    }

    private static String formatMultiplier(double value) {
        return value <= 0.0 ? "1.0" : String.format("%.1f", value);
    }
}
