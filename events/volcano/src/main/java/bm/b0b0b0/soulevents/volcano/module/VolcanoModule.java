package bm.b0b0b0.soulevents.volcano.module;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.api.module.EventModuleContext;
import bm.b0b0b0.soulevents.api.schedule.ScheduleSpec;
import bm.b0b0b0.soulevents.volcano.VolcanoPlugin;
import bm.b0b0b0.soulevents.volcano.command.VolcanoCommand;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPluginConfig;
import bm.b0b0b0.soulevents.volcano.config.VolcanoTypeDefinition;
import bm.b0b0b0.soulevents.volcano.gate.WorldPlacementGate;
import bm.b0b0b0.soulevents.volcano.gui.VolcanoGuiFactory;
import bm.b0b0b0.soulevents.volcano.integration.VaultEconomyService;
import bm.b0b0b0.soulevents.volcano.integration.WorldGuardIntegrations;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.volcano.service.VolcanoDespawnBossBarService;
import bm.b0b0b0.soulevents.volcano.service.VolcanoService;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Optional;

public final class VolcanoModule implements EventModule {

    public static final String MODULE_ID = "volcano";

    private final VolcanoPlugin plugin;
    private final SoulEventsApi api;
    private VolcanoPluginConfig config;
    private VolcanoMessageService messages;
    private VolcanoService service;
    private VolcanoGuiFactory guiFactory;

    public VolcanoModule(
            VolcanoPlugin plugin,
            SoulEventsApi api,
            VolcanoPluginConfig config,
            VolcanoMessageService messages
    ) {
        this.plugin = plugin;
        this.api = api;
        this.config = config;
        this.messages = messages;
        this.service = new VolcanoService(api, plugin, config, messages);
        VaultEconomyService vaultEconomy = new VaultEconomyService(plugin);
        vaultEconomy.hook();
        service.setVaultEconomy(vaultEconomy);
        service.setDespawnBossBarService(new VolcanoDespawnBossBarService(plugin, messages));
    }

    public VolcanoService service() {
        return service;
    }

    public void setGuiFactory(VolcanoGuiFactory guiFactory) {
        this.guiFactory = guiFactory;
    }

    public void reload(VolcanoPluginConfig config, VolcanoMessageService messages, VolcanoGuiFactory guiFactory) {
        this.config = config;
        this.messages = messages;
        this.guiFactory = guiFactory;
        service.reload(config);
        VaultEconomyService vaultEconomy = new VaultEconomyService(plugin);
        vaultEconomy.hook();
        service.setVaultEconomy(vaultEconomy);
        service.setDespawnBossBarService(new VolcanoDespawnBossBarService(plugin, messages));
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
        for (VolcanoTypeDefinition definition : config.types()) {
            if (!definition.settings().enabled || definition.settings().intervalMinutes <= 0L) {
                continue;
            }
            String typeId = definition.id();
            WorldPlacementGate gate = new WorldPlacementGate(
                    definition.settings().worldPlacement,
                    WorldGuardIntegrations.createProbe()
            );
            ScheduleSpec schedule = new ScheduleSpec(
                    true,
                    Duration.ofMinutes(definition.settings().intervalMinutes),
                    gate.schedulerWorlds(),
                    definition.settings().maxConcurrent
            );
            api.scheduler().register(MODULE_ID, typeId, schedule, () -> service.spawnScheduled(typeId));
        }
        new VolcanoCommand(api, service, messages, guiFactory, plugin).register(plugin);
    }

    @Override
    public void onDisable() {
        for (VolcanoTypeDefinition definition : config.types()) {
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
