package bm.b0b0b0.soulevents.volcano.config;

import bm.b0b0b0.soulevents.volcano.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoModuleSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Map;

public final class VolcanoConfigurationLoader {

    private VolcanoConfigurationLoader() {
    }

    public static VolcanoPluginConfig load(JavaPlugin plugin) {
        VolcanoModuleSettings module = new VolcanoModuleSettings();
        SerializedConfigReloader.reload(plugin, module, Path.of("config.yml"));

        GuiGeneralSettings gui = new GuiGeneralSettings();
        SerializedConfigReloader.reload(plugin, gui, Path.of("gui", "general.yml"));

        Map<String, VolcanoTypeDefinition> types = TypeDirectoryLoader.load(plugin);
        return new VolcanoPluginConfig(module, gui, types);
    }
}
