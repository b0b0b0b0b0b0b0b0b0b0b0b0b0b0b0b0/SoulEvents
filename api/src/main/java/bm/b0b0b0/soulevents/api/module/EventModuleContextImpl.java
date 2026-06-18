package bm.b0b0b0.soulevents.api.module;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

public final class EventModuleContextImpl implements EventModuleContext {

    private final SoulEventsApi api;
    private final Plugin plugin;

    public EventModuleContextImpl(SoulEventsApi api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
    }

    @Override
    public SoulEventsApi api() {
        return api;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public Path dataFolder() {
        return plugin.getDataFolder().toPath();
    }
}
