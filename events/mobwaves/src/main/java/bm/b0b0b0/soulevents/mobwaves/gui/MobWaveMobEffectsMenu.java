package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.ProfileDirectoryLoader;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobEffectsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobPotionEffectSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.MobPotionEffectSupport;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntityNames;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class MobWaveMobEffectsMenu implements InventoryHolder {

    private final MobWavesPlugin plugin;
    private final MobWavesPluginConfig config;
    private final MobWaveMessageService messages;
    private final MobWavesGuiFactory guiFactory;
    private final String profileId;
    private final EntityType entityType;
    private final Inventory inventory;
    private final Map<Integer, Integer> slotEffectIndex = new HashMap<>();

    public MobWaveMobEffectsMenu(
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
        MobEffectsGuiSettings gui = config.gui().mobEffects;
        Component title = messages.resolve("gui.mob-effects.title-prefix", Map.of())
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
        MobEffectsGuiSettings gui = config.gui().mobEffects;
        int slot = event.getRawSlot();
        if (slot == gui.backSlot) {
            guiFactory.openMobOverride(player, profileId, entityType == null ? "ZOMBIE" : entityType.name());
            return;
        }
        if (slot == gui.infoSlot) {
            return;
        }
        Optional<MobTypeOverrideSettings> overrideOptional = currentOverride();
        if (overrideOptional.isEmpty()) {
            return;
        }
        MobTypeOverrideSettings override = overrideOptional.get();
        ensureEffectsList(override);
        if (slot == gui.addSlot) {
            addEffect(override);
            save(override);
            render();
            return;
        }
        Integer effectIndex = slotEffectIndex.get(slot);
        if (effectIndex == null || effectIndex < 0 || effectIndex >= override.effects.size()) {
            return;
        }
        MobPotionEffectSettings entry = override.effects.get(effectIndex);
        if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_LEFT) {
            override.effects.remove((int) effectIndex);
        } else {
            entry.amplifier = Math.min(4, entry.amplifier + 1);
        }
        save(override);
        render();
    }

    private void addEffect(MobTypeOverrideSettings override) {
        String seed = override.effects.isEmpty() ? "SPEED" : override.effects.getLast().type;
        PotionEffectType next = MobPotionEffectSupport.nextGuiType(seed, override.effects);
        MobPotionEffectSettings entry = new MobPotionEffectSettings();
        entry.type = next.getKey().getKey().toUpperCase();
        entry.amplifier = 0;
        entry.durationTicks = -1;
        override.effects.add(entry);
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
        ensureEffectsList(override);
        profileOptional.get().settings().mobOverrides.put(entityType.name(), override);
        ProfileDirectoryLoader.save(plugin, profileOptional.get());
    }

    private static void ensureEffectsList(MobTypeOverrideSettings override) {
        if (override.effects == null) {
            override.effects = new ArrayList<>();
        }
    }

    private void render() {
        GuiFrames.fillBackground(inventory);
        slotEffectIndex.clear();
        MobTypeOverrideSettings override = currentOverride().orElse(new MobTypeOverrideSettings());
        ensureEffectsList(override);
        MobEffectsGuiSettings gui = config.gui().mobEffects;
        Map<String, String> ph = Map.of(
                "profile", profileId,
                "count", Integer.toString(override.effects.size())
        );
        inventory.setItem(gui.infoSlot, GuiIcons.icon(
                Material.matchMaterial(gui.infoMaterial),
                messages.resolve("gui.mob-effects.info", ph),
                messages.resolveLore("gui.mob-effects.info-lore", ph)
        ));
        inventory.setItem(gui.addSlot, GuiIcons.icon(
                Material.matchMaterial(gui.addMaterial),
                messages.resolve("gui.mob-effects.add", ph),
                messages.resolveLore("gui.mob-effects.add-lore", ph)
        ));
        int slot = gui.listStartSlot;
        for (int index = 0; index < override.effects.size(); index++) {
            if (slot > gui.listEndSlot) {
                break;
            }
            MobPotionEffectSettings entry = override.effects.get(index);
            PotionEffectType type = MobPotionEffectSupport.resolve(entry.type);
            inventory.setItem(slot, GuiIcons.icon(
                    MobPotionEffectSupport.iconMaterial(type),
                    type == null
                            ? Component.text(entry.type)
                            : MobPotionEffectSupport.displayName(type, entry.amplifier),
                    messages.resolveLore("gui.mob-effects.entry-lore", Map.of(
                            "summary", MobPotionEffectSupport.formatSummary(entry)
                    ))
            ));
            slotEffectIndex.put(slot, index);
            slot++;
        }
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.mob-effects.back", ph),
                messages.resolveLore("gui.mob-effects.back-lore", ph)
        ));
    }
}
