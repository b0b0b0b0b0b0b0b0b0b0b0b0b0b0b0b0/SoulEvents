package bm.b0b0b0.soulevents.mobwaves;

import bm.b0b0b0.soulevents.api.SoulEventsApi;
import bm.b0b0b0.soulevents.api.module.EventModuleContextImpl;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesConfigurationLoader;
import bm.b0b0b0.soulevents.mobwaves.config.MobWavesPluginConfig;
import bm.b0b0b0.soulevents.mobwaves.gui.MobWavesGuiListener;
import bm.b0b0b0.soulevents.mobwaves.listener.ArenaRegionEnforcementListener;
import bm.b0b0b0.soulevents.mobwaves.listener.MobHordeItemPickupListener;
import bm.b0b0b0.soulevents.mobwaves.listener.MobWaveListener;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
import bm.b0b0b0.soulevents.mobwaves.message.MobWavesStartupConsolePresenter;
import bm.b0b0b0.soulevents.mobwaves.module.MobWavesModule;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobWavesPlugin extends JavaPlugin {

    private SoulEventsApi api;
    private MobWavesPluginConfig config;
    private MobWaveMessageService messages;
    private MobWavesModule module;
    private MobWaveListener mobWaveListener;
    private MobWavesStartupConsolePresenter startupConsole;

    @Override
    public void onEnable() {
        reloadLocalConfig();
        startupConsole = new MobWavesStartupConsolePresenter(this, messages);
        RegisteredServiceProvider<SoulEventsApi> provider =
                Bukkit.getServicesManager().getRegistration(SoulEventsApi.class);
        if (provider == null) {
            startupConsole.logCoreMissing();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        api = provider.getProvider();
        logLoadedContent();
        module = new MobWavesModule(this, api, config, messages);
        module.onLoad(new EventModuleContextImpl(api, this));
        api.modules().register(module);
        getServer().getPluginManager().registerEvents(new MobWavesGuiListener(messages), this);
        mobWaveListener = new MobWaveListener(this, module.waveService(), config);
        getServer().getPluginManager().registerEvents(mobWaveListener, this);
        getServer().getPluginManager().registerEvents(
                new MobHordeItemPickupListener(module.hordeService()),
                this
        );
        getServer().getPluginManager().registerEvents(
                new ArenaRegionEnforcementListener(module.hordeService(), config, module.hordeService().arenaRegions()),
                this
        );
        module.onEnable();
        startupConsole.logRegistered(config.types().size(), config.profiles().size());
    }

    public void reloadLocalConfig() {
        config = MobWavesConfigurationLoader.load(this);
        messages = new MobWaveMessageService(this, config.module().locale);
        messages.load();
    }

    public void reloadAll() {
        reloadLocalConfig();
        messages.load();
        startupConsole = new MobWavesStartupConsolePresenter(this, messages);
        logLoadedContent();
        if (module != null) {
            module.reload(config, messages);
            module.onReload();
        }
        if (mobWaveListener != null) {
            mobWaveListener.reload(config);
        }
    }

    public MobWavesModule module() {
        return module;
    }

    private void logLoadedContent() {
        if (startupConsole == null || config == null) {
            return;
        }
        config.typesById().keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(startupConsole::logTypeLoaded);
        config.profiles().stream()
                .map(profile -> profile.id())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(startupConsole::logProfileLoaded);
    }

    @Override
    public void onDisable() {
        if (module != null) {
            module.onDisable();
            api.modules().unregister(module.id());
        }
    }
}
