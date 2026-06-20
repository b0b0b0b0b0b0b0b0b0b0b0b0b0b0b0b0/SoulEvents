package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.volcano.config.VolcanoTypeDefinition;
import bm.b0b0b0.soulevents.volcano.config.settings.VolcanoLifecycleSettings;
import bm.b0b0b0.soulevents.volcano.message.VolcanoMessageService;
import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VolcanoDespawnBossBarService implements Listener {

    private static final BossBar.Color WAITING_BAR_COLOR = BossBar.Color.YELLOW;
    private static final BossBar.Color EXTINGUISH_BAR_COLOR = BossBar.Color.RED;

    private final Plugin plugin;
    private final VolcanoMessageService messages;
    private Supplier<Map<UUID, VolcanoSessionRegistry.SessionRecord>> sessionSnapshot = Map::of;
    private Function<UUID, Optional<ActiveEvent>> activeEventLookup = sessionId -> Optional.empty();
    private Function<String, Optional<VolcanoTypeDefinition>> typeLookup = typeId -> Optional.empty();
    private BukkitTask tickTask;
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    public VolcanoDespawnBossBarService(Plugin plugin, VolcanoMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void wire(
            Supplier<Map<UUID, VolcanoSessionRegistry.SessionRecord>> sessionSnapshot,
            Function<UUID, Optional<ActiveEvent>> activeEventLookup,
            Function<String, Optional<VolcanoTypeDefinition>> typeLookup
    ) {
        this.sessionSnapshot = sessionSnapshot;
        this.activeEventLookup = activeEventLookup;
        this.typeLookup = typeLookup;
    }

    public void start() {
        stop();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        for (Map.Entry<UUID, BossBar> entry : playerBars.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.hideBossBar(entry.getValue());
            }
        }
        playerBars.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        removeBar(event.getPlayer());
    }

    private void tick() {
        Set<UUID> visiblePlayers = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            findBestSession(player).ifPresentOrElse(
                    session -> {
                        visiblePlayers.add(player.getUniqueId());
                        updateBar(player, session);
                    },
                    () -> removeBar(player)
            );
        }
        playerBars.keySet().removeIf(playerId -> {
            if (visiblePlayers.contains(playerId)) {
                return false;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                BossBar bar = playerBars.get(playerId);
                if (bar != null) {
                    player.hideBossBar(bar);
                }
            }
            return true;
        });
    }

    private Optional<SessionView> findBestSession(Player player) {
        SessionView best = null;
        Instant now = Instant.now();
        for (var entry : sessionSnapshot.get().entrySet()) {
            UUID sessionId = entry.getKey();
            VolcanoSessionRegistry.SessionRecord record = entry.getValue();
            Optional<ActiveEvent> active = activeEventLookup.apply(sessionId);
            if (active.isEmpty()) {
                continue;
            }
            Optional<VolcanoTypeDefinition> definition = typeLookup.apply(active.get().typeId());
            if (definition.isEmpty()) {
                continue;
            }
            VolcanoLifecycleSettings lifecycle = definition.get().settings().lifecycle;
            if (!lifecycle.bossBarEnabled || lifecycle.bossBarRadius <= 0) {
                continue;
            }
            Location anchor = record.ventAnchor().clone().add(0.5, 0.5, 0.5);
            if (anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
                continue;
            }
            double radius = lifecycle.bossBarRadius;
            if (player.getLocation().distanceSquared(anchor) > radius * radius) {
                continue;
            }

            Optional<SessionView> candidate = Optional.empty();
            if (now.isBefore(record.eruptAt())) {
                long remaining = Math.max(0L, Duration.between(now, record.eruptAt()).toSeconds());
                candidate = Optional.of(new SessionView(
                        definition.get(),
                        record.eruptAt(),
                        remaining,
                        BarMode.WAITING
                ));
            } else if (now.isBefore(record.extinguishAt())) {
                long remaining = Math.max(0L, Duration.between(now, record.extinguishAt()).toSeconds());
                candidate = Optional.of(new SessionView(
                        definition.get(),
                        record.extinguishAt(),
                        remaining,
                        BarMode.EXTINGUISH
                ));
            }

            if (candidate.isPresent()) {
                SessionView view = candidate.get();
                if (best == null || view.remainingSeconds() < best.remainingSeconds()) {
                    best = view;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private void updateBar(Player player, SessionView session) {
        VolcanoLifecycleSettings lifecycle = session.definition().settings().lifecycle;
        long totalSeconds = switch (session.mode()) {
            case WAITING -> Math.max(1L, session.definition().settings().lifecycle.eruptDelaySeconds);
            case EXTINGUISH -> Math.max(1L, session.definition().settings().lifecycle.maxActiveSeconds);
        };
        float progress = Math.max(0F, Math.min(1F, session.remainingSeconds() / (float) totalSeconds));
        BossBar.Color color = session.mode() == BarMode.WAITING ? WAITING_BAR_COLOR : EXTINGUISH_BAR_COLOR;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("timer", formatTimer(session.remainingSeconds()));
        placeholders.put(
                "type_name",
                messages.resolvePlain(session.definition().settings().displayNameKey, Map.of())
        );
        String messageKey = session.mode() == BarMode.WAITING
                ? lifecycle.bossBarWaitingKey
                : lifecycle.bossBarExtinguishKey;
        Component title = messages.resolve(messageKey, placeholders);
        BossBar bar = playerBars.computeIfAbsent(player.getUniqueId(), ignored -> BossBar.bossBar(
                title,
                progress,
                color,
                BossBar.Overlay.NOTCHED_20
        ));
        bar.name(title);
        bar.progress(progress);
        bar.color(color);
        player.showBossBar(bar);
    }

    private void removeBar(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        if (bar != null) {
            player.hideBossBar(bar);
        }
    }

    private static String formatTimer(long seconds) {
        long minutes = seconds / 60L;
        long rest = seconds % 60L;
        return String.format("%02d:%02d", minutes, rest);
    }

    private enum BarMode {
        WAITING,
        EXTINGUISH
    }

    private record SessionView(
            VolcanoTypeDefinition definition,
            Instant endAt,
            long remainingSeconds,
            BarMode mode
    ) {
    }
}
