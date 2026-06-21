package bm.b0b0b0.soulevents.api;

import bm.b0b0b0.soulevents.api.message.MessageService;
import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;
import bm.b0b0b0.soulevents.api.module.EventSessionController;
import bm.b0b0b0.soulevents.api.protection.ProtectionServices;
import bm.b0b0b0.soulevents.api.schedule.EventScheduler;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import org.bukkit.plugin.Plugin;

public interface SoulEventsApi {

    Plugin plugin();

    FlatSurfaceFinder placement();

    EventModuleRegistry modules();

    EventSessionController sessions();

    EventScheduler scheduler();

    ProtectionServices protection();

    SchematicService schematics();

    MessageService messages();

    PlayerEventStatsService stats();
}
