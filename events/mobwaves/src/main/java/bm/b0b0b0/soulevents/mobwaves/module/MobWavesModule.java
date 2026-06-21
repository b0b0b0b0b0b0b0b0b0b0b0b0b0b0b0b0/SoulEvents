package bm.b0b0b0.soulevents.mobwaves.module;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.api.module.EventModuleContext;
import bm.b0b0b0.soulevents.api.schedule.ScheduleSpec;
import bm.b0b0b0.soulevents.api.world.WorldGuardProbe;
import bm.b0b0b0.soulevents.mobwaves.MobWavesPlugin;
import bm.b0b0b0.soulevents.mobwaves.command.MobWavesCommand;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.mobwaves.gui.MobWavesGuiFactory;
import bm.b0b0b0.soulevents.mobwaves.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.service.HordePlayerEnforcementService;
import bm.b0b0b0.soulevents.mobwaves.service.HordeMobTargetingService;
import bm.b0b0b0.soulevents.mobwaves.service.HordeMobVisualService;
import bm.b0b0b0.soulevents.mobwaves.service.MobHealthBarService;
import bm.b0b0b0.soulevents.mobwaves.service.MobHordeService;
import bm.b0b0b0.soulevents.mobwaves.service.MobLootDropService;
import bm.b0b0b0.soulevents.mobwaves.service.MobWaveBridgeImpl;
import bm.b0b0b0.soulevents.mobwaves.service.MobWaveService;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.time.Duration;
import java.util.Optional;

public final class MobWavesModule implements EventModule {

    public static final String MODULE_ID = "mobwaves";

    private final MobWavesPlugin plugin;
    private final SoulEventsApi api;
    private MobWavesPluginConfig config;
    private MobWaveMessageService messages;
    private MobWaveService waveService;
    private MobHealthBarService healthBarService;
    private HordeMobVisualService mobVisualService;
    private HordeMobTargetingService targetingService;
    private HordePlayerEnforcementService playerEnforcementService;
    private MobWaveBridgeImpl bridge;
    private MobHordeService hordeService;
    private MobWavesGuiFactory guiFactory;

    public MobWavesModule(
            MobWavesPlugin plugin,
            SoulEventsApi api,
            MobWavesPluginConfig config,
            MobWaveMessageService messages
    ) {
        this.plugin = plugin;
        this.api = api;
        this.config = config;
        this.messages = messages;
        this.healthBarService = new MobHealthBarService(plugin, messages);
        this.waveService = new MobWaveService(plugin, config, healthBarService, api);
        this.mobVisualService = new HordeMobVisualService(plugin, config);
        this.mobVisualService.bindWaveService(waveService);
        this.waveService.bindMobVisualService(mobVisualService);
        this.bridge = new MobWaveBridgeImpl(waveService);
        MobLootDropService lootDropService = new MobLootDropService(plugin, messages);
        this.hordeService = new MobHordeService(api, plugin, config, messages, waveService, lootDropService);
        this.targetingService = new HordeMobTargetingService(
                plugin,
                config,
                waveService,
                hordeService.nexusVisualService()
        );
        this.playerEnforcementService = new HordePlayerEnforcementService(plugin, config, hordeService, waveService);
        this.guiFactory = new MobWavesGuiFactory(plugin, config, messages, hordeService);
    }

    public MobWaveService waveService() {
        return waveService;
    }

    public MobHordeService hordeService() {
        return hordeService;
    }

    public MobWaveBridgeImpl bridge() {
        return bridge;
    }

    public MobWavesGuiFactory guiFactory() {
        return guiFactory;
    }

    public void reload(MobWavesPluginConfig config, MobWaveMessageService messages) {
        this.config = config;
        this.messages = messages;
        waveService.reload(config);
        mobVisualService.reload(config);
        targetingService.reload(config);
        hordeService.reload(config);
        playerEnforcementService.reload(config);
        guiFactory.reload(config);
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
        healthBarService.start();
        mobVisualService.start();
        targetingService.start();
        playerEnforcementService.start();
        hordeService.startBossBar();
        plugin.getServer().getServicesManager().register(
                bm.b0b0b0.soulevents.api.mobwave.MobWaveBridge.class,
                bridge,
                plugin,
                ServicePriority.Normal
        );
        registerSchedulers();
        new MobWavesCommand(plugin, messages, guiFactory, hordeService).register(plugin);
    }

    @Override
    public void onDisable() {
        for (HordeTypeDefinition definition : config.types()) {
            api.scheduler().unregister(MODULE_ID, definition.id());
        }
        plugin.getServer().getServicesManager().unregisterAll(plugin);
        hordeService.shutdown();
        waveService.shutdown();
        targetingService.stop();
        mobVisualService.stop();
        playerEnforcementService.stop();
        healthBarService.stop();
    }

    @Override
    public void onReload() {
        for (HordeTypeDefinition definition : config.types()) {
            api.scheduler().unregister(MODULE_ID, definition.id());
        }
        registerSchedulers();
    }

    @Override
    public Optional<bm.b0b0b0.soulevents.api.module.EventTypeDefinition> definition() {
        return Optional.empty();
    }

    private void registerSchedulers() {
        WorldGuardProbe probe = WorldGuardIntegrations.createProbe();
        for (HordeTypeDefinition definition : config.types()) {
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
            api.scheduler().register(MODULE_ID, typeId, schedule, () -> hordeService.spawnScheduled(typeId));
        }
    }
}
