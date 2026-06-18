package bm.b0b0b0.soulevents.api.module;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;

public interface EventModuleContext {

    SoulEventsApi api();

    Plugin plugin();

    Path dataFolder();
}
