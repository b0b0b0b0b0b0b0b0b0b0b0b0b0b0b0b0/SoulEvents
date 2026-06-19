package bm.b0b0b0.soulevents.volcano.gui;

import bm.b0b0b0.soulevents.volcano.VolcanoPlugin;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPluginConfig;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.volcano.service.VolcanoService;
import org.bukkit.entity.Player;

public final class VolcanoGuiFactory {

    private final VolcanoPlugin plugin;
    private final VolcanoPluginConfig config;
    private final VolcanoMessageService messages;
    private final VolcanoService service;

    public VolcanoGuiFactory(
            VolcanoPlugin plugin,
            VolcanoPluginConfig config,
            VolcanoMessageService messages,
            VolcanoService service
    ) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.service = service;
    }

    public void openAdmin(Player player) {
        new VolcanoAdminMenu(config, messages, this).open(player);
    }

    public void openTypeSettings(Player player, String typeId) {
        new VolcanoTypeSettingsMenu(config, messages, service, this, typeId).open(player);
    }

    public void openCreate(Player player) {
        new VolcanoCreateMenu(plugin, config, messages, this).open(player);
    }

    public void openLootHub(Player player, String typeId) {
        new VolcanoLootHubMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openObfuscationItems(Player player, String typeId) {
        new VolcanoObfuscationItemsMenu(plugin, config, messages, this, typeId).open(player);
    }

    public void openLootPool(Player player, String typeId, int page) {
        new VolcanoLootPoolMenu(plugin, config, messages, this, typeId, page).open(player);
    }
}

