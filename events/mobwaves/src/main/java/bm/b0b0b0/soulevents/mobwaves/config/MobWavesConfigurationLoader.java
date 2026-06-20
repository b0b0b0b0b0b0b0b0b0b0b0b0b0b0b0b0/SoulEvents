package bm.b0b0b0.soulevents.mobwaves.config;

import bm.b0b0b0.soulevents.mobwaves.config.settings.GuiGeneralSettings;
import bm.b0b0b0.soulevents.mobwaves.config.settings.MobWavesModuleSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;
import java.util.Map;

public final class MobWavesConfigurationLoader {

    private MobWavesConfigurationLoader() {
    }

    public static MobWavesPluginConfig load(JavaPlugin plugin) {
        MobWavesModuleSettings module = new MobWavesModuleSettings();
        SerializedConfigReloader.reload(plugin, module, Path.of("config.yml"));

        GuiGeneralSettings gui = new GuiGeneralSettings();
        SerializedConfigReloader.reload(plugin, gui, Path.of("gui", "general.yml"));

        Map<String, WaveProfileDefinition> profiles = ProfileDirectoryLoader.load(plugin);
        Map<String, HordeTypeDefinition> types = TypeDirectoryLoader.load(plugin);
        return new MobWavesPluginConfig(module, gui, profiles, types);
    }
}
