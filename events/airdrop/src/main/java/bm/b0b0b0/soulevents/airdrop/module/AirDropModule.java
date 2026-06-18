package bm.b0b0b0.soulevents.airdrop.module;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.api.module.EventModuleContext;
import bm.b0b0b0.soulevents.api.schedule.ScheduleSpec;
import bm.b0b0b0.soulevents.airdrop.AirDropPlugin;
import bm.b0b0b0.soulevents.airdrop.command.AirDropCommand;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.airdrop.gui.AirDropGuiFactory;
import bm.b0b0b0.soulevents.airdrop.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.repository.SqlAirDropSessionRepository;
import bm.b0b0b0.soulevents.airdrop.service.AirDropService;
import bm.b0b0b0.soulevents.airdrop.service.AirDropVisualService;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;

public final class AirDropModule implements EventModule {

    public static final String MODULE_ID = "airdrop";

    private final AirDropPlugin plugin;
    private final SoulEventsApi api;
    private AirDropPluginConfig config;
    private AirDropMessageService messages;
    private AirDropService service;
    private AirDropGuiFactory guiFactory;
    private final SqlAirDropSessionRepository sessionRepository;

    public AirDropModule(
            AirDropPlugin plugin,
            SoulEventsApi api,
            AirDropPluginConfig config,
            AirDropMessageService messages,
            SqlAirDropSessionRepository sessionRepository
    ) {
        this.plugin = plugin;
        this.api = api;
        this.config = config;
        this.messages = messages;
        this.sessionRepository = sessionRepository;
        this.service = new AirDropService(api, plugin, config, messages, sessionRepository);
        service.setVisualService(new AirDropVisualService(plugin, messages));
    }

    public AirDropService service() {
        return service;
    }

    public void setGuiFactory(AirDropGuiFactory guiFactory) {
        this.guiFactory = guiFactory;
    }

    public void reload(AirDropPluginConfig config, AirDropMessageService messages, AirDropGuiFactory guiFactory) {
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        service.setVisualService(new AirDropVisualService(plugin, messages));
        service.reload(config);
    }

    @Override
    public String id() {
        return MODULE_ID;
    }

    @Override
    public Plugin plugin() {
        return plugin;
    }

    @Override
    public void onLoad(EventModuleContext context) {
    }

    @Override
    public void onEnable() {
        WorldGuardProbe probe = WorldGuardIntegrations.createProbe();
        for (AirDropTypeDefinition definition : config.types()) {
            if (!definition.settings().enabled || definition.settings().intervalMinutes <= 0L) {
                continue;
            }
            String typeId = definition.id();
            WorldPlacementGate gate = new WorldPlacementGate(definition.settings().worldPlacement, probe);
            ScheduleSpec schedule = new ScheduleSpec(
                    true,
                    Duration.ofMinutes(definition.settings().intervalMinutes),
                    gate.schedulerWorlds(),
                    definition.settings().maxConcurrent
            );
            api.scheduler().register(MODULE_ID, typeId, schedule, () -> service.spawnScheduled(typeId));
        }
        new AirDropCommand(api, service, messages, guiFactory, plugin).register(plugin);
    }

    @Override
    public void onDisable() {
        for (AirDropTypeDefinition definition : config.types()) {
            api.scheduler().unregister(MODULE_ID, definition.id());
        }
        service.shutdown();
    }

    @Override
    public void onReload() {
        onDisable();
        onEnable();
    }

    @Override
    public Optional<bm.b0b0b0.soulevents.api.module.EventTypeDefinition> definition() {
        return Optional.empty();
    }
}
