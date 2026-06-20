package bm.b0b0b0.soulevents.mobwaves.gui;

import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.config.WaveProfileDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobSettingsGuiSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobTypeOverrideSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.util.MobWaveEntitySupport;
import net.kyori.adventure.text.Component;
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
        String mobType = slotTypes.get(event.getRawSlot());
        if (mobType != null) {
            guiFactory.openMobOverride(player, profileId, mobType);
        }
    }

    private void render() {
        inventory.clear();
        slotTypes.clear();
        MobSettingsGuiSettings gui = config.gui().mobSettings;
        inventory.setItem(gui.backSlot, GuiIcons.icon(
                Material.matchMaterial(gui.backMaterial),
                messages.resolve("gui.mob-settings.back", Map.of()),
                List.of()
        ));
        Optional<WaveProfileDefinition> profileOptional = config.profile(profileId);
        if (profileOptional.isEmpty()) {
            return;
        }
        WaveProfileDefinition profile = profileOptional.get();
        int slot = gui.mobListStartSlot;
        for (EntityType type : MobWaveEntitySupport.allowedTypes()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            MobTypeOverrideSettings override = profile.settings().mobOverrides.getOrDefault(
                    type.name(),
                    new MobTypeOverrideSettings()
            );
            String health = override.maxHealth > 0.0
                    ? Integer.toString((int) override.maxHealth)
                    : "default";
            ItemStack egg = new ItemStack(MobWaveEntitySupport.spawnEgg(type));
            egg.editMeta(meta -> {
                meta.displayName(Component.text(type.name()));
                meta.lore(messages.resolveLore("gui.mob-settings.entry-lore", Map.of(
                        "health", health,
                        "speed", formatMultiplier(override.speedMultiplier),
                        "damage", formatMultiplier(override.damageMultiplier)
                )));
            });
            inventory.setItem(slot, egg);
            slotTypes.put(slot, type.name());
            slot++;
        }
    }

    private static String formatMultiplier(double value) {
        return value <= 0.0 ? "1.0" : String.format("%.1f", value);
    }
}
