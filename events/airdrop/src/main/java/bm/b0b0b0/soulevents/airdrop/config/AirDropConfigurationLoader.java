package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.AirDropModuleSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.GuiGeneralSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Map;

public final class AirDropConfigurationLoader {

    private AirDropConfigurationLoader() {
    }

    public static AirDropPluginConfig load(JavaPlugin plugin) {
        AirDropModuleSettings module = new AirDropModuleSettings();
        SerializedConfigReloader.reload(plugin, module, Path.of("config.yml"));

        GuiGeneralSettings gui = new GuiGeneralSettings();
        SerializedConfigReloader.reload(plugin, gui, Path.of("gui", "general.yml"));

        Map<String, AirDropTypeDefinition> types = TypeDirectoryLoader.load(plugin);
        return new AirDropPluginConfig(module, gui, types);
    }
}
