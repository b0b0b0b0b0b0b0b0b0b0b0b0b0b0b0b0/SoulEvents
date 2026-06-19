package bm.b0b0b0.soulevents.airdrop.gui;

import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
import org.bukkit.entity.Player;

public final class AirDropGuiFactory {

    private final AirDropPlugin plugin;
    private final AirDropPluginConfig config;
    private final AirDropMessageService messages;
    private final AirDropService service;

    public AirDropGuiFactory(
            AirDropPlugin plugin,
            AirDropPluginConfig config,
            AirDropMessageService messages,
            AirDropService service
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.service = service;
    }

    public void openAdmin(Player player) {
        new AirDropAdminMenu(config, messages, this).open(player);
    }

    public void openTypeSettings(Player player, String typeId) {
        new AirDropTypeSettingsMenu(config, messages, service, this, typeId).open(player);
    }

    public void openRequirements(Player player, String typeId) {
        new AirDropRequirementsMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openRequiredItems(Player player, String typeId) {
        new AirDropRequiredItemsMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openCreate(Player player) {
        new AirDropCreateMenu(plugin, config, messages, this).open(player);
    }

    public void openLootHub(Player player, String typeId) {
        new AirDropLootHubMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openObfuscationItems(Player player, String typeId) {
        new AirDropObfuscationItemsMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openLootPool(Player player, String typeId, int page) {
        new AirDropLootPoolMenu(plugin, config, messages, this, typeId, page).open(player);
    }
}
