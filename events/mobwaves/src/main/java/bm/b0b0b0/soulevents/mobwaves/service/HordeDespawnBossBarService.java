package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.api.mobwave.MobWaveChestPhase;
import bm.b0b0b0.soulevents.mobwaves.config.HordeTypeDefinition;
import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeLifecycleSettings;
import bm.b0b0b0.soulevents.mobwaves.message.MobWaveMessageService;
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

public final class HordeDespawnBossBarService implements Listener {

    private static final BossBar.Color WAITING_BAR_COLOR = BossBar.Color.YELLOW;
    private static final BossBar.Color ACTIVE_BAR_COLOR = BossBar.Color.GREEN;
    private static final BossBar.Color DESPAWN_BAR_COLOR = BossBar.Color.RED;
    private static final BossBar.Color WAVE_BAR_COLOR = BossBar.Color.WHITE;
    private static final BossBar.Color VICTORY_BAR_COLOR = BossBar.Color.PURPLE;

    private final Plugin plugin;
    private final MobWaveMessageService messages;
    private Supplier<Map<UUID, MobHordeSessionRegistry.SessionRecord>> sessionSnapshot = Map::of;
    private Function<UUID, Optional<ActiveEvent>> activeEventLookup = sessionId -> Optional.empty();
    private Function<String, Optional<HordeTypeDefinition>> typeLookup = typeId -> Optional.empty();
    private Function<UUID, Optional<MobWaveService.WaveTimerSnapshot>> waveTimerLookup = sessionId -> Optional.empty();
    private BukkitTask tickTask;
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    public HordeDespawnBossBarService(Plugin plugin, MobWaveMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void wire(
            Supplier<Map<UUID, MobHordeSessionRegistry.SessionRecord>> sessionSnapshot,
            Function<UUID, Optional<ActiveEvent>> activeEventLookup,
            Function<String, Optional<HordeTypeDefinition>> typeLookup,
            Function<UUID, Optional<MobWaveService.WaveTimerSnapshot>> waveTimerLookup
    ) {
        this.sessionSnapshot = sessionSnapshot;
        this.activeEventLookup = activeEventLookup;
        this.typeLookup = typeLookup;
        this.waveTimerLookup = waveTimerLookup;
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
            MobHordeSessionRegistry.SessionRecord record = entry.getValue();
            Optional<ActiveEvent> active = activeEventLookup.apply(sessionId);
            if (active.isEmpty()) {
                continue;
            }
            Optional<HordeTypeDefinition> definition = typeLookup.apply(active.get().typeId());
            if (definition.isEmpty()) {
                continue;
            }
            HordeLifecycleSettings lifecycle = definition.get().settings().lifecycle;
            if (!lifecycle.bossBarEnabled || lifecycle.bossBarRadius <= 0) {
                continue;
            }
            Location anchor = record.waveAnchor().clone().add(0.5, 0.5, 0.5);
            if (anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
                continue;
            }
            double radius = lifecycle.bossBarRadius;
            if (player.getLocation().distanceSquared(anchor) > radius * radius) {
                continue;
            }

            Optional<SessionView> candidate = Optional.empty();
            Optional<MobWaveService.WaveTimerSnapshot> waveTimer = waveTimerLookup.apply(sessionId);
            if (record.wavesAttached() && waveTimer.isPresent()) {
                MobWaveService.WaveTimerSnapshot timer = waveTimer.get();
                if (timer.phase() == MobWaveChestPhase.GRACE) {
                    candidate = Optional.of(SessionView.waveGrace(
                            definition.get(),
                            timer.remainingSeconds(),
                            timer.displayMaxSeconds(),
                            timer.waveIndex(),
                            timer.waveCount(),
                            timer.bonusSecondsPerKill()
                    ));
                } else {
                    candidate = Optional.of(SessionView.wave(
                            definition.get(),
                            timer.remainingSeconds(),
                            timer.displayMaxSeconds(),
                            timer.waveIndex(),
                            timer.waveCount(),
                            timer.bonusSecondsPerKill()
                    ));
                }
            } else {
                Instant endAt = record.endAt();
                if (endAt != null && now.isBefore(endAt)) {
                    long remaining = Math.max(0L, Duration.between(now, endAt).toSeconds());
                    if (record.wavesVictory()) {
                        candidate = Optional.of(SessionView.victory(definition.get(), remaining));
                    } else {
                        candidate = Optional.of(SessionView.despawn(definition.get(), remaining));
                    }
                } else if (!record.wavesAttached()) {
                    candidate = Optional.of(SessionView.waiting(definition.get()));
                } else {
                    Instant expireAt = record.expireAt();
                    if (expireAt != null && now.isBefore(expireAt)) {
                        long remaining = Math.max(0L, Duration.between(now, expireAt).toSeconds());
                        candidate = Optional.of(SessionView.active(
                                definition.get(),
                                remaining,
                                lifecycle.maxActiveSeconds
                        ));
                    }
                }
            }

            if (candidate.isPresent()) {
                SessionView view = candidate.get();
                if (best == null || view.priority() < best.priority()) {
                    best = view;
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private void updateBar(Player player, SessionView session) {
        HordeLifecycleSettings lifecycle = session.definition().settings().lifecycle;
        float progress = switch (session.mode()) {
            case WAITING -> 1F;
            case WAVE, WAVE_GRACE, ACTIVE, DESPAWN, VICTORY -> Math.max(
                    0F,
                    Math.min(1F, session.remainingSeconds() / (float) Math.max(1L, session.displayMaxSeconds()))
            );
        };
        BossBar.Color color = switch (session.mode()) {
            case WAITING -> WAITING_BAR_COLOR;
            case WAVE -> WAVE_BAR_COLOR;
            case WAVE_GRACE -> BossBar.Color.GREEN;
            case ACTIVE -> ACTIVE_BAR_COLOR;
            case DESPAWN -> DESPAWN_BAR_COLOR;
            case VICTORY -> VICTORY_BAR_COLOR;
        };
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("timer", formatTimer(session.remainingSeconds()));
        placeholders.put(
                "type_name",
                messages.resolvePlain(session.definition().settings().displayNameKey, Map.of())
        );
        placeholders.put("wave", Integer.toString(session.waveIndex()));
        placeholders.put("waves", Integer.toString(session.waveCount()));
        placeholders.put("next_wave", Integer.toString(Math.min(session.waveCount(), session.waveIndex() + 1)));
        placeholders.put("bonus", formatTimer(session.bonusSecondsPerKill()));
        String messageKey = switch (session.mode()) {
            case WAITING -> lifecycle.bossBarWaitingKey;
            case WAVE -> lifecycle.bossBarWaveKey;
            case WAVE_GRACE -> lifecycle.bossBarWaveGraceKey;
            case ACTIVE -> lifecycle.bossBarActiveKey;
            case DESPAWN -> lifecycle.bossBarDespawnKey;
            case VICTORY -> lifecycle.bossBarVictoryKey;
        };
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
        WAVE(-1),
        WAVE_GRACE(-1),
        WAITING(0),
        ACTIVE(1),
        VICTORY(1),
        DESPAWN(2);

        private final int priority;

        BarMode(int priority) {
            this.priority = priority;
        }

        int priority() {
            return priority;
        }
    }

    private record SessionView(
            HordeTypeDefinition definition,
            long remainingSeconds,
            long displayMaxSeconds,
            int waveIndex,
            int waveCount,
            int bonusSecondsPerKill,
            BarMode mode
    ) {
        static SessionView wave(
                HordeTypeDefinition definition,
                long remainingSeconds,
                long displayMaxSeconds,
                int waveIndex,
                int waveCount,
                int bonusSecondsPerKill
        ) {
            return new SessionView(
                    definition,
                    remainingSeconds,
                    displayMaxSeconds,
                    waveIndex,
                    waveCount,
                    bonusSecondsPerKill,
                    BarMode.WAVE
            );
        }

        static SessionView waveGrace(
                HordeTypeDefinition definition,
                long remainingSeconds,
                long displayMaxSeconds,
                int waveIndex,
                int waveCount,
                int bonusSecondsPerKill
        ) {
            return new SessionView(
                    definition,
                    remainingSeconds,
                    displayMaxSeconds,
                    waveIndex,
                    waveCount,
                    bonusSecondsPerKill,
                    BarMode.WAVE_GRACE
            );
        }

        static SessionView waiting(HordeTypeDefinition definition) {
            return new SessionView(definition, 0L, 1L, 0, 0, 0, BarMode.WAITING);
        }

        static SessionView active(HordeTypeDefinition definition, long remainingSeconds, long displayMaxSeconds) {
            return new SessionView(definition, remainingSeconds, displayMaxSeconds, 0, 0, 0, BarMode.ACTIVE);
        }

        static SessionView despawn(HordeTypeDefinition definition, long remainingSeconds) {
            return new SessionView(
                    definition,
                    remainingSeconds,
                    Math.max(1L, definition.settings().lifecycle.maxActiveSecondsAfterCleared),
                    0,
                    0,
                    0,
                    BarMode.DESPAWN
            );
        }

        static SessionView victory(HordeTypeDefinition definition, long remainingSeconds) {
            return new SessionView(
                    definition,
                    remainingSeconds,
                    Math.max(1L, definition.settings().lifecycle.maxActiveSecondsAfterCleared),
                    0,
                    0,
                    0,
                    BarMode.VICTORY
            );
        }

        int priority() {
            return mode.priority();
        }
    }
}
