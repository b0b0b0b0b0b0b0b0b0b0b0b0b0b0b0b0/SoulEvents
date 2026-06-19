package bm.b0b0b0.soulevents.core;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.core.api.SoulEventsApiImpl;
import bm.b0b0b0.soulevents.core.command.SoulEventsCommand;
import bm.b0b0b0.soulevents.core.config.ConfigurationLoader;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import bm.b0b0b0.soulevents.core.listener.ProtectionListener;
import bm.b0b0b0.soulevents.core.listener.VirtualLootProtectionListener;
import bm.b0b0b0.soulevents.core.message.StartupConsolePresenter;
import bm.b0b0b0.soulevents.core.message.StartupCoordinator;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class SoulEventsPlugin extends JavaPlugin {

    private SoulEventsApiImpl api;
    private PluginConfig pluginConfig;
    private StartupConsolePresenter startupConsole;
    private StartupCoordinator startupCoordinator;

    @Override
    public void onEnable() {
        pluginConfig = ConfigurationLoader.load(this);
        api = new SoulEventsApiImpl(this, pluginConfig);
        startupConsole = new StartupConsolePresenter(this, api.yamlMessages());
        startupCoordinator = new StartupCoordinator(this, api.modules(), startupConsole);
        api.modules().setRegisterListener(module -> startupCoordinator.onModuleRegistered(module.id()));
        startupConsole.logStartupHeader();
        getServer().getServicesManager().register(SoulEventsApi.class, api, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(new ProtectionListener(api), this);
        getServer().getPluginManager().registerEvents(new VirtualLootProtectionListener(), this);
        new SoulEventsCommand(api, api.schematicService()).register(this);
        startupCoordinator.scheduleFallbackFooter(200L);
    }

    @Override
    public void onDisable() {
        if (api != null) {
            for (EventModule module : api.modules().modules()) {
                module.onDisable();
            }
            getServer().getServicesManager().unregisterAll(this);
            api.shutdown();
        }
    }

    public void reloadAll() {
        pluginConfig = ConfigurationLoader.load(this);
        api.reload(pluginConfig);
        for (EventModule module : api.modules().modules()) {
            module.onReload();
        }
        startupConsole.logReloadComplete();
    }
}
