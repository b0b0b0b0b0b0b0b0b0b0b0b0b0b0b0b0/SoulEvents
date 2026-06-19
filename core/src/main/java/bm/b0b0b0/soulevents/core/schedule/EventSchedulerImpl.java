package bm.b0b0b0.soulevents.core.schedule;

import bm.b0b0b0.soulevents.api.schedule.EventScheduler;
import bm.b0b0b0.soulevents.api.schedule.ScheduleSpec;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class EventSchedulerImpl implements EventScheduler {

    private final Plugin plugin;
    private final Map<String, ScheduledEntry> entries = new ConcurrentHashMap<>();

    public EventSchedulerImpl(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register(String moduleId, String typeId, ScheduleSpec spec, Runnable trigger) {
        String key = key(moduleId, typeId);
        cancelEntry(key);
        if (!spec.enabled()) {
            return;
        }
        long intervalTicks = Math.max(1L, spec.interval().toSeconds() * 20L);
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, trigger, intervalTicks, intervalTicks);
        entries.put(key, new ScheduledEntry(moduleId, typeId, spec, trigger, task, System.currentTimeMillis()));
    }

    @Override
    public void unregister(String moduleId, String typeId) {
        cancelEntry(key(moduleId, typeId));
    }

    @Override
    public void triggerNow(String moduleId, String typeId) {
        ScheduledEntry entry = entries.get(key(moduleId, typeId));
        if (entry != null) {
            entry.trigger().run();
        }
    }

    @Override
    public Optional<Duration> timeUntilNext(String moduleId, String typeId) {
        ScheduledEntry entry = entries.get(key(moduleId, typeId));
        if (entry == null) {
            return Optional.empty();
        }
        long intervalMillis = entry.spec().interval().toMillis();
        if (intervalMillis <= 0) {
            return Optional.empty();
        }
        long elapsed = System.currentTimeMillis() - entry.startedAtMillis();
        long remaining = intervalMillis - (elapsed % intervalMillis);
        return Optional.of(Duration.ofMillis(remaining));
    }

    public void reloadAll() {
        List<ScheduledEntry> snapshot = List.copyOf(entries.values());
        for (ScheduledEntry scheduled : snapshot) {
            cancelEntry(key(scheduled.moduleId(), scheduled.typeId()));
            register(scheduled.moduleId(), scheduled.typeId(), scheduled.spec(), scheduled.trigger());
        }
    }

    public void cancelAll() {
        for (String key : List.copyOf(entries.keySet())) {
            cancelEntry(key);
        }
    }

    private void cancelEntry(String key) {
        ScheduledEntry entry = entries.remove(key);
        if (entry != null) {
            entry.task().cancel();
        }
    }

    private static String key(String moduleId, String typeId) {
        return moduleId + ':' + typeId;
    }

    private record ScheduledEntry(
            String moduleId,
            String typeId,
            ScheduleSpec spec,
            Runnable trigger,
            BukkitTask task,
            long startedAtMillis
    ) {
    }
}
