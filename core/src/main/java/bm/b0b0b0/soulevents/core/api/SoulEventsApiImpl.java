package bm.b0b0b0.soulevents.core.api;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.message.MessageService;
import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;
import bm.b0b0b0.soulevents.api.module.EventSessionController;
import bm.b0b0b0.soulevents.api.protection.ProtectionServices;
import bm.b0b0b0.soulevents.api.schedule.EventScheduler;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import bm.b0b0b0.soulevents.core.message.YamlMessageService;
import bm.b0b0b0.soulevents.core.module.EventModuleRegistryImpl;
import bm.b0b0b0.soulevents.core.module.EventSessionControllerImpl;
import bm.b0b0b0.soulevents.core.placeholder.PlaceholderApiHook;
import bm.b0b0b0.soulevents.core.protection.ProtectionServicesImpl;
import bm.b0b0b0.soulevents.core.schedule.EventSchedulerImpl;
import bm.b0b0b0.soulevents.core.schematic.SchematicServiceImpl;
import bm.b0b0b0.soulevents.core.stats.PlayerEventStatsServiceImpl;
import bm.b0b0b0.soulevents.core.world.FlatSurfaceFinderImpl;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulEventsApiImpl implements SoulEventsApi {

    private final Plugin plugin;
    private final EventModuleRegistryImpl modules;
    private final EventSessionControllerImpl sessions;
    private final PlayerEventStatsServiceImpl stats;
    private final EventSchedulerImpl scheduler;
    private final ProtectionServicesImpl protection;
    private final SchematicServiceImpl schematics;
    private final YamlMessageService messages;
    private final FlatSurfaceFinderImpl placement = new FlatSurfaceFinderImpl();

    public SoulEventsApiImpl(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.sessions = new EventSessionControllerImpl();
        this.modules = new EventModuleRegistryImpl(sessions);
        this.stats = new PlayerEventStatsServiceImpl((JavaPlugin) plugin, config, sessions);
        this.scheduler = new EventSchedulerImpl(plugin);
        this.protection = new ProtectionServicesImpl(plugin, config, stats);
        this.messages = new YamlMessageService(plugin);
        this.schematics = new SchematicServiceImpl((JavaPlugin) plugin, this.messages);
    }

    public void start() {
        stats.start();
        PlaceholderApiHook.hook(plugin, stats);
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public FlatSurfaceFinder placement() {
        return placement;
    }

    @Override
    public EventModuleRegistry modules() {
        return modules;
    }

    @Override
    public EventSessionController sessions() {
        return sessions;
    }

    @Override
    public EventScheduler scheduler() {
        return scheduler;
    }

    @Override
    public ProtectionServices protection() {
        return protection;
    }

    @Override
    public SchematicService schematics() {
        return schematics;
    }

    @Override
    public MessageService messages() {
        return messages;
    }

    @Override
    public PlayerEventStatsService stats() {
        return stats;
    }

    public YamlMessageService yamlMessages() {
        return messages;
    }

    public SchematicServiceImpl schematicService() {
        return schematics;
    }

    public PlayerEventStatsServiceImpl statsService() {
        return stats;
    }

    public void reload(PluginConfig config) {
        protection.reload(config);
        stats.reload(config);
        schematics.reload();
        messages.reload();
        scheduler.reloadAll();
    }

    public void shutdown() {
        scheduler.cancelAll();
        sessions.clear();
        protection.shutdown();
        stats.shutdown();
        schematics.shutdown();
    }
}
