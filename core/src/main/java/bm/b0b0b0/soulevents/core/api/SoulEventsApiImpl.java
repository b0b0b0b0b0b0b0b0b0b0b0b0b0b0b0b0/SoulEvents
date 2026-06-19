package bm.b0b0b0.soulevents.core.api;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.message.MessageService;
import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;
import bm.b0b0b0.soulevents.api.module.EventSessionController;
import bm.b0b0b0.soulevents.api.protection.ProtectionServices;
import bm.b0b0b0.soulevents.api.schedule.EventScheduler;
import bm.b0b0b0.soulevents.api.schematic.SchematicService;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import bm.b0b0b0.soulevents.core.world.FlatSurfaceFinderImpl;
import bm.b0b0b0.soulevents.core.message.YamlMessageService;
import bm.b0b0b0.soulevents.core.module.EventModuleRegistryImpl;
import bm.b0b0b0.soulevents.core.module.EventSessionControllerImpl;
import bm.b0b0b0.soulevents.core.protection.ProtectionServicesImpl;
import bm.b0b0b0.soulevents.core.schedule.EventSchedulerImpl;
import bm.b0b0b0.soulevents.core.schematic.SchematicServiceImpl;
import org.bukkit.plugin.Plugin;

public final class SoulEventsApiImpl implements SoulEventsApi {

    private final Plugin plugin;
    private final EventModuleRegistryImpl modules;
    private final EventSessionControllerImpl sessions;
    private final EventSchedulerImpl scheduler;
    private final ProtectionServicesImpl protection;
    private final SchematicServiceImpl schematics;
    private final YamlMessageService messages;
    private final FlatSurfaceFinderImpl placement = new FlatSurfaceFinderImpl();

    public SoulEventsApiImpl(Plugin plugin, PluginConfig config) {
        this.plugin = plugin;
        this.sessions = new EventSessionControllerImpl();
        this.modules = new EventModuleRegistryImpl(sessions);
        this.scheduler = new EventSchedulerImpl(plugin);
        this.protection = new ProtectionServicesImpl(plugin, config);
        this.schematics = new SchematicServiceImpl(plugin);
        this.messages = new YamlMessageService(plugin);
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

    public YamlMessageService yamlMessages() {
        return messages;
    }

    public SchematicServiceImpl schematicService() {
        return schematics;
    }

    public void reload(PluginConfig config) {
        protection.reload(config);
        schematics.reload();
        messages.reload();
        scheduler.reloadAll();
    }

    public void shutdown() {
        scheduler.cancelAll();
        sessions.clear();
        protection.shutdown();
        schematics.shutdown();
    }
}
