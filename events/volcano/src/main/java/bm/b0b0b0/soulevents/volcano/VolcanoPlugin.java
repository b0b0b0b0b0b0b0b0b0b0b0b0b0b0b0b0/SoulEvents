package bm.b0b0b0.soulevents.volcano;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModuleContextImpl;
import bm.b0b0b0.soulevents.volcano.config.VolcanoConfigurationLoader;
import bm.b0b0b0.soulevents.volcano.config.VolcanoPluginConfig;
import bm.b0b0b0.soulevents.volcano.gui.VolcanoGuiFactory;
import bm.b0b0b0.soulevents.volcano.gui.VolcanoGuiListener;
import bm.b0b0b0.soulevents.volcano.listener.ArenaRegionEnforcementListener;
import bm.b0b0b0.soulevents.volcano.listener.VolcanoItemPickupListener;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.volcano.message.VolcanoStartupConsolePresenter;
import bm.b0b0b0.soulevents.volcano.module.VolcanoModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VolcanoPlugin extends JavaPlugin {

    private SoulEventsApi api;
    private VolcanoPluginConfig config;
    private VolcanoMessageService messages;
    private VolcanoModule module;
    private VolcanoGuiFactory guiFactory;
    private VolcanoStartupConsolePresenter startupConsole;

    @Override
    public void onEnable() {
        reloadLocalConfig();
        startupConsole = new VolcanoStartupConsolePresenter(this, messages);
        RegisteredServiceProvider<SoulEventsApi> provider =
                Bukkit.getServicesManager().getRegistration(SoulEventsApi.class);
        if (provider == null) {
            startupConsole.logCoreMissing();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        api = provider.getProvider();
        startModule();
    }

    private void startModule() {
        logLoadedTypes();
        module = new VolcanoModule(this, api, config, messages);
        guiFactory = new VolcanoGuiFactory(this, config, messages, module.service());
        module.setGuiFactory(guiFactory);
        module.onLoad(new EventModuleContextImpl(api, this));
        api.modules().register(module);
        getServer().getPluginManager().registerEvents(new VolcanoGuiListener(messages), this);
        getServer().getPluginManager().registerEvents(new VolcanoItemPickupListener(module.service()), this);
        getServer().getPluginManager().registerEvents(
                new ArenaRegionEnforcementListener(
                        module.service(),
                        config,
                        module.service().arenaRegions()
                ),
                this
        );
        module.onEnable();
    }

    public void reloadLocalConfig() {
        config = VolcanoConfigurationLoader.load(this);
        messages = new VolcanoMessageService(this, config.module().locale);
        messages.load();
    }

    public void reloadAll() {
        reloadLocalConfig();
        startupConsole = new VolcanoStartupConsolePresenter(this, messages);
        logLoadedTypes();
        if (module != null) {
            guiFactory = new VolcanoGuiFactory(this, config, messages, module.service());
            module.setGuiFactory(guiFactory);
            module.reload(config, messages, guiFactory);
            module.onReload();
        }
    }

    public VolcanoMessageService messages() {
        return messages;
    }

    public VolcanoGuiFactory guiFactory() {
        return guiFactory;
    }

    private void logLoadedTypes() {
        if (startupConsole == null || config == null) {
            return;
        }
        for (String typeId : config.typesById().keySet()) {
            startupConsole.logTypeLoaded(typeId);
        }
    }

    @Override
    public void onDisable() {
        if (module != null) {
            module.onDisable();
            api.modules().unregister(module.id());
        }
    }
}
