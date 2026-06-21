package bm.b0b0b0.soulevents.core.stats;

import bm.b0b0b0.soulevents.api.module.EventSessionController;
import bm.b0b0b0.soulevents.api.stats.EventStatsMetrics;
import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import bm.b0b0b0.soulevents.core.config.PluginConfig;
import bm.b0b0b0.soulevents.core.config.settings.StatsSettings;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class PlayerEventStatsServiceImpl implements PlayerEventStatsService {

    private final JavaPlugin plugin;
    private final EventSessionController sessions;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "SoulEvents-Stats");
        thread.setDaemon(true);
        return thread;
    });

    private StatsRepository repository;
    private StatsSettings settings;
    private BukkitTask flushTask;
    private final Map<PlayerStatKey, Long> totals = new ConcurrentHashMap<>();
    private final Map<PlayerStatKey, Long> pending = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Object pendingLock = new Object();

    public PlayerEventStatsServiceImpl(JavaPlugin plugin, PluginConfig config, EventSessionController sessions) {
        this.plugin = plugin;
        this.sessions = sessions;
        applyConfig(config);
    }

    public void start() {
        if (!settings.enabled || repository == null) {
            return;
        }
        scheduleFlush();
    }

    public void reload(PluginConfig config) {
        flushNow();
        shutdownTasks();
        applyConfig(config);
        if (settings.enabled && repository != null) {
            scheduleFlush();
        }
    }

    public void shutdown() {
        shutdownTasks();
        flushNow();
        if (repository != null) {
            repository.close();
            repository = null;
        }
        executor.shutdown();
        totals.clear();
        pending.clear();
        loadedPlayers.clear();
    }

    @Override
    public void increment(UUID playerId, String moduleId, String typeId, String metric, long delta) {
        if (!enabled() || playerId == null || delta == 0L || moduleId == null || metric == null) {
            return;
        }
        ensureLoaded(playerId);
        String scope = EventStatsMetrics.normalizeTypeScope(typeId);
        addDelta(new PlayerStatKey(playerId, moduleId, scope, metric), delta);
        if (!EventStatsMetrics.SCOPE_GLOBAL.equals(scope)) {
            addDelta(new PlayerStatKey(playerId, moduleId, EventStatsMetrics.SCOPE_GLOBAL, metric), delta);
        }
    }

    @Override
    public void incrementGlobal(UUID playerId, String moduleId, String metric, long delta) {
        if (!enabled() || playerId == null || delta == 0L || moduleId == null || metric == null) {
            return;
        }
        ensureLoaded(playerId);
        addDelta(new PlayerStatKey(playerId, moduleId, EventStatsMetrics.SCOPE_GLOBAL, metric), delta);
    }

    @Override
    public void recordSession(UUID playerId, UUID sessionId, String metric, long delta) {
        if (!enabled() || playerId == null || sessionId == null) {
            return;
        }
        sessions.get(sessionId).ifPresent(event ->
                increment(playerId, event.moduleId(), event.typeId(), metric, delta)
        );
    }

    @Override
    public long cached(UUID playerId, String moduleId, String scopeId, String metric) {
        if (playerId == null || moduleId == null || metric == null) {
            return 0L;
        }
        ensureLoaded(playerId);
        PlayerStatKey key = new PlayerStatKey(
                playerId,
                moduleId,
                EventStatsMetrics.normalizeScope(scopeId),
                metric
        );
        return totals.getOrDefault(key, 0L) + pendingValue(key);
    }

    private long pendingValue(PlayerStatKey key) {
        synchronized (pendingLock) {
            return pending.getOrDefault(key, 0L);
        }
    }

    @Override
    public long cachedModuleTotal(UUID playerId, String moduleId, String metric) {
        return cached(playerId, moduleId, EventStatsMetrics.SCOPE_GLOBAL, metric);
    }

    @Override
    public long cachedGrandTotal(UUID playerId, String metric) {
        if (playerId == null || metric == null) {
            return 0L;
        }
        long sum = 0L;
        for (String moduleId : EventStatsMetrics.MODULES) {
            sum += cachedModuleTotal(playerId, moduleId, metric);
        }
        return sum;
    }

    @Override
    public CompletableFuture<Long> load(UUID playerId, String moduleId, String scopeId, String metric) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        if (!enabled() || repository == null || playerId == null) {
            future.complete(0L);
            return future;
        }
        executor.execute(() -> {
            try {
                ensureLoadedSync(playerId);
                future.complete(cached(playerId, moduleId, scopeId, metric));
            } catch (Exception exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    @Override
    public void ensureReady(UUID playerId) {
        if (!enabled() || playerId == null || loadedPlayers.contains(playerId)) {
            return;
        }
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    ensureLoadedSync(playerId);
                } catch (Exception exception) {
                    plugin.getLogger().warning("SoulEvents stats load failed for " + playerId + ": " + exception.getMessage());
                }
            }, executor).get(250L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException exception) {
            plugin.getLogger().fine("SoulEvents stats load timeout for " + playerId);
        } catch (Exception exception) {
            plugin.getLogger().warning("SoulEvents stats load failed for " + playerId + ": " + exception.getMessage());
        }
    }

    @Override
    public void flushNow() {
        if (!enabled() || repository == null) {
            return;
        }
        Map<PlayerStatKey, Long> batch;
        synchronized (pendingLock) {
            if (pending.isEmpty()) {
                return;
            }
            batch = new HashMap<>(pending);
            pending.clear();
        }
        executor.execute(() -> {
            try {
                repository.flush(batch);
                mergeIntoTotals(batch);
            } catch (Exception exception) {
                plugin.getLogger().warning("SoulEvents stats flush failed: " + exception.getMessage());
                requeue(batch);
            }
        });
    }

    private void applyConfig(PluginConfig config) {
        this.settings = config.stats();
        if (!settings.enabled) {
            if (repository != null) {
                repository.close();
                repository = null;
            }
            return;
        }
        try {
            if (repository != null) {
                repository.close();
            }
            repository = new StatsRepository(plugin, settings);
        } catch (Exception exception) {
            plugin.getLogger().severe("SoulEvents stats database init failed: " + exception.getMessage());
            repository = null;
        }
    }

    private void scheduleFlush() {
        int interval = Math.max(5, settings.flushIntervalSeconds);
        flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::flushNow,
                interval * 20L,
                interval * 20L
        );
    }

    private void shutdownTasks() {
        if (flushTask != null) {
            flushTask.cancel();
            flushTask = null;
        }
    }

    private boolean enabled() {
        return settings.enabled && repository != null;
    }

    private void addDelta(PlayerStatKey key, long delta) {
        synchronized (pendingLock) {
            pending.merge(key, delta, Long::sum);
        }
    }

    private void mergeIntoTotals(Map<PlayerStatKey, Long> batch) {
        for (Map.Entry<PlayerStatKey, Long> entry : batch.entrySet()) {
            totals.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }

    private void requeue(Map<PlayerStatKey, Long> batch) {
        synchronized (pendingLock) {
            for (Map.Entry<PlayerStatKey, Long> entry : batch.entrySet()) {
                pending.merge(entry.getKey(), entry.getValue(), Long::sum);
            }
        }
    }

    private void ensureLoaded(UUID playerId) {
        if (loadedPlayers.contains(playerId)) {
            return;
        }
        executor.execute(() -> {
            try {
                ensureLoadedSync(playerId);
            } catch (Exception exception) {
                plugin.getLogger().warning("SoulEvents stats load failed for " + playerId + ": " + exception.getMessage());
            }
        });
    }

    public void warmPlayer(UUID playerId) {
        if (!enabled() || playerId == null || loadedPlayers.contains(playerId)) {
            return;
        }
        executor.execute(() -> {
            try {
                ensureLoadedSync(playerId);
            } catch (Exception exception) {
                plugin.getLogger().warning("SoulEvents stats warm failed for " + playerId + ": " + exception.getMessage());
            }
        });
    }

    private void ensureLoadedSync(UUID playerId) throws Exception {
        if (!loadedPlayers.add(playerId)) {
            return;
        }
        Map<PlayerStatKey, Long> loaded = repository.loadPlayer(playerId);
        for (Map.Entry<PlayerStatKey, Long> entry : loaded.entrySet()) {
            totals.merge(entry.getKey(), entry.getValue(), Long::sum);
        }
    }
}
