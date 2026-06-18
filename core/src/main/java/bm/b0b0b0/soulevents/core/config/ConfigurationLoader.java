package bm.b0b0b0.soulevents.core.config;

import bm.b0b0b0.soulevents.core.config.settings.CoreSettings;
import bm.b0b0b0.soulevents.core.config.settings.ProtectionSettings;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

public final class ConfigurationLoader {

    private ConfigurationLoader() {
    }

    public static PluginConfig load(Plugin plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        CoreSettings core = new CoreSettings();
        core.reload(dataFolder.resolve("config.yml"));
        ProtectionSettings protection = new ProtectionSettings();
        protection.reload(dataFolder.resolve("protection.yml"));
        return new PluginConfig(core, protection);
    }
}
