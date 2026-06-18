package bm.b0b0b0.soulevents.core.config;

import bm.b0b0b0.soulevents.core.config.settings.CoreSettings;
import bm.b0b0b0.soulevents.core.config.settings.ProtectionSettings;
import org.bukkit.plugin.Plugin;

public final class PluginConfig {

    private final CoreSettings core;
    private final ProtectionSettings protection;

    public PluginConfig(CoreSettings core, ProtectionSettings protection) {
        this.core = core;
        this.protection = protection;
    }

    public CoreSettings core() {
        return core;
    }

    public ProtectionSettings protection() {
        return protection;
    }

    public static PluginConfig defaults() {
        return new PluginConfig(new CoreSettings(), new ProtectionSettings());
    }
}
