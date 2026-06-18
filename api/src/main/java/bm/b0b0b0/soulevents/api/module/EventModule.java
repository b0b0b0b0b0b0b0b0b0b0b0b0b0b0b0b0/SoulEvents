package bm.b0b0b0.soulevents.api.module;

import org.bukkit.plugin.Plugin;

import java.util.Optional;

public interface EventModule {

    String id();

    Plugin plugin();

    void onLoad(EventModuleContext context);

    void onEnable();

    void onDisable();

    void onReload();

    Optional<EventTypeDefinition> definition();
}
