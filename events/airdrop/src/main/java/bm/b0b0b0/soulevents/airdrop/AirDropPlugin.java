package bm.b0b0b0.soulevents.airdrop;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.airdrop.config.AirDropConfigurationLoader;
import bm.b0b0b0.soulevents.airdrop.config.AirDropPluginConfig;
import bm.b0b0b0.soulevents.airdrop.config.DatabaseConfig;
import bm.b0b0b0.soulevents.airdrop.database.AsyncDatabaseExecutor;
import bm.b0b0b0.soulevents.airdrop.database.DataSourceProvider;
import bm.b0b0b0.soulevents.airdrop.database.DatabaseBootstrap;
import bm.b0b0b0.soulevents.airdrop.gui.AirDropGuiFactory;
import bm.b0b0b0.soulevents.airdrop.gui.AirDropGuiListener;
import bm.b0b0b0.soulevents.airdrop.listener.ArenaRegionEnforcementListener;
import bm.b0b0b0.soulevents.airdrop.listener.AirDropChestListener;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
import bm.b0b0b0.soulevents.airdrop.message.AirDropStartupConsolePresenter;
import bm.b0b0b0.soulevents.airdrop.module.AirDropModule;
import bm.b0b0b0.soulevents.airdrop.repository.SqlAirDropSessionRepository;
import bm.b0b0b0.soulevents.api.module.EventModuleContextImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class AirDropPlugin extends JavaPlugin {

    private SoulEventsApi api;
    private AirDropPluginConfig config;
    private AirDropMessageService messages;
    private AsyncDatabaseExecutor databaseExecutor;
    private DatabaseBootstrap databaseBootstrap;
    private SqlAirDropSessionRepository sessionRepository;
    private AirDropModule module;
    private AirDropGuiFactory guiFactory;
    private AirDropStartupConsolePresenter startupConsole;

    @Override
    public void onEnable() {
        reloadLocalConfig();
        startupConsole = new AirDropStartupConsolePresenter(this, messages);
        RegisteredServiceProvider<SoulEventsApi> provider =
                Bukkit.getServicesManager().getRegistration(SoulEventsApi.class);
        if (provider == null) {
            startupConsole.logCoreMissing();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        api = provider.getProvider();
        databaseExecutor = new AsyncDatabaseExecutor();
        DataSourceProvider dataSourceProvider = new DataSourceProvider();
        databaseBootstrap = new DatabaseBootstrap(
                this,
                new DatabaseConfig(config.module().database),
                dataSourceProvider,
                databaseExecutor
        );
        databaseBootstrap.start().thenAccept(success -> Bukkit.getScheduler().runTask(this, () -> {
            if (!success) {
                startupConsole.logDatabaseFailed();
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }
            sessionRepository = new SqlAirDropSessionRepository(dataSourceProvider, databaseExecutor);
            startModule();
        }));
    }

    private void startModule() {
        module = new AirDropModule(this, api, config, messages, sessionRepository);
        guiFactory = new AirDropGuiFactory(this, config, messages, module.service());
        module.setGuiFactory(guiFactory);
        module.onLoad(new EventModuleContextImpl(api, this));
        api.modules().register(module);
        getServer().getPluginManager().registerEvents(new AirDropGuiListener(messages), this);
        getServer().getPluginManager().registerEvents(new AirDropChestListener(module.service()), this);
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
        config = AirDropConfigurationLoader.load(this);
        messages = new AirDropMessageService(this, config.module().locale);
        messages.load();
    }

    public void reloadAll() {
        reloadLocalConfig();
        startupConsole = new AirDropStartupConsolePresenter(this, messages);
        if (module != null) {
            guiFactory = new AirDropGuiFactory(this, config, messages, module.service());
            module.reload(config, messages, guiFactory);
            module.onReload();
        }
    }

    public AirDropMessageService messages() {
        return messages;
    }

    public AirDropGuiFactory guiFactory() {
        return guiFactory;
    }

    @Override
    public void onDisable() {
        if (module != null) {
            module.onDisable();
            api.modules().unregister(module.id());
        }
        if (databaseBootstrap != null) {
            databaseBootstrap.shutdown();
        }
        if (databaseExecutor != null) {
            databaseExecutor.shutdown();
        }
    }
}
