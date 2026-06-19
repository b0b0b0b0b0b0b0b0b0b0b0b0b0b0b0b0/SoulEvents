package bm.b0b0b0.soulevents.core.message;

import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class StartupCoordinator {

    private final JavaPlugin plugin;
    private final EventModuleRegistry moduleRegistry;
    private final StartupConsolePresenter presenter;
    private boolean initializationStarted;
    private boolean integrationsLogged;
    private boolean footerLogged;
    private int footerTaskId = -1;

    public StartupCoordinator(
            JavaPlugin plugin,
            EventModuleRegistry moduleRegistry,
            StartupConsolePresenter presenter
    ) {
        this.plugin = plugin;
        this.moduleRegistry = moduleRegistry;
        this.presenter = presenter;
    }

    public void scheduleFallbackFooter(long delayTicks) {
        scheduleFooter(delayTicks);
    }

    public void onModuleRegistered(String moduleId) {
        if (footerLogged) {
            return;
        }
        if (!initializationStarted) {
            presenter.beginInitialization();
            initializationStarted = true;
        }
        if (!integrationsLogged) {
            logIntegrations();
            integrationsLogged = true;
        }
        presenter.logModule(moduleId);
        scheduleFooter(1L);
    }

    private void logStartupFooterNow() {
        if (footerLogged) {
            return;
        }
        cancelFooterTask();
        if (!initializationStarted) {
            presenter.beginInitialization();
            if (!integrationsLogged) {
                logIntegrations();
                integrationsLogged = true;
            }
        }
        presenter.logStartupFooter(moduleRegistry);
        footerLogged = true;
    }

    private void scheduleFooter(long delayTicks) {
        cancelFooterTask();
        footerTaskId = Bukkit.getScheduler().runTaskLater(
                plugin,
                this::logStartupFooterNow,
                delayTicks
        ).getTaskId();
    }

    private void cancelFooterTask() {
        if (footerTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(footerTaskId);
            footerTaskId = -1;
        }
    }

    private void logIntegrations() {
        Plugin worldGuard = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null && worldGuard.isEnabled()) {
            presenter.logIntegration(
                    "startup.integration.worldguard",
                    Map.of("version", worldGuard.getPluginMeta().getVersion())
            );
            return;
        }
        if (worldGuard != null) {
            presenter.logIntegration(
                    "startup.integration.worldguard-loaded",
                    Map.of("version", worldGuard.getPluginMeta().getVersion())
            );
            return;
        }
        presenter.logIntegration("startup.integration.worldguard-missing", Map.of());
    }
}
