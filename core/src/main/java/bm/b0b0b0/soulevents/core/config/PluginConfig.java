package bm.b0b0b0.soulevents.core.config;

import bm.b0b0b0.soulevents.core.config.settings.CoreSettings;
import bm.b0b0b0.soulevents.core.config.settings.ProtectionSettings;
import bm.b0b0b0.soulevents.core.config.settings.StatsSettings;

public final class PluginConfig {

    private final CoreSettings core;
    private final ProtectionSettings protection;
    private final StatsSettings stats;

    public PluginConfig(CoreSettings core, ProtectionSettings protection, StatsSettings stats) {
        this.core = core;
        this.protection = protection;
        this.stats = stats;
    }

    public CoreSettings core() {
        return core;
    }

    public ProtectionSettings protection() {
        return protection;
    }

    public StatsSettings stats() {
        return stats;
    }

    public static PluginConfig defaults() {
        return new PluginConfig(new CoreSettings(), new ProtectionSettings(), new StatsSettings());
    }
}
