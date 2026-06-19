package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.config.AirDropTypeDefinition;
import bm.b0b0b0.soulevents.airdrop.config.settings.LifecycleSettings;
import bm.b0b0b0.soulevents.airdrop.message.AirDropMessageService;
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

public final class AirDropDespawnBossBarService implements Listener {

    private static final String[] PULSE_SYMBOLS = {"⚠", "☢", "⏱", "◆", "▣", "✦"};
    private static final String[][] CALM_GRADIENTS = {
            {"#4B5320", "#FFD700"},
            {"#556B2F", "#FFA500"},
            {"#6B8E23", "#FF8C00"},
            {"#808000", "#FFD700"},
            {"#8B8000", "#FF6347"},
            {"#556B2F", "#DAA520"},
            {"#4B5320", "#FF4500"},
            {"#6B8E23", "#FFC107"},
    };
    private static final String[][] CRITICAL_GRADIENTS = {
            {"#FF0000", "#FFD700"},
            {"#FF4500", "#FF0000"},
            {"#DC143C", "#FFA500"},
            {"#B22222", "#FFFF00"},
    };
    private static final String[][] LOOTED_GRADIENTS = {
            {"#F59E0B", "#EF4444"},
            {"#FB923C", "#F87171"},
            {"#FFD700", "#FF4500"},
            {"#FF8C00", "#DC143C"},
    };

    private final Plugin plugin;
    private final AirDropMessageService messages;
    private Supplier<Map<UUID, AirDropSessionRegistry.SessionRecord>> sessionSnapshot = Map::of;
    private Function<UUID, Optional<ActiveEvent>> activeEventLookup = sessionId -> Optional.empty();
    private Function<String, Optional<AirDropTypeDefinition>> typeLookup = typeId -> Optional.empty();
    private BukkitTask tickTask;
    private final Map<UUID, BossBar> playerBars = new HashMap<>();

    public AirDropDespawnBossBarService(Plugin plugin, AirDropMessageService messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void wire(
            Supplier<Map<UUID, AirDropSessionRegistry.SessionRecord>> sessionSnapshot,
            Function<UUID, Optional<ActiveEvent>> activeEventLookup,
            Function<String, Optional<AirDropTypeDefinition>> typeLookup
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
        for (var entry : sessionSnapshot.get().entrySet()) {
            UUID sessionId = entry.getKey();
            AirDropSessionRegistry.SessionRecord record = entry.getValue();
            Optional<ActiveEvent> active = activeEventLookup.apply(sessionId);
            if (active.isEmpty()) {
                continue;
            }
            Optional<AirDropTypeDefinition> definition = typeLookup.apply(active.get().typeId());
            if (definition.isEmpty()) {
                continue;
            }
            LifecycleSettings lifecycle = definition.get().settings().lifecycle;
            if (!lifecycle.bossBarEnabled || lifecycle.bossBarRadius <= 0) {
                continue;
            }
            Instant lootableAt = active.get().lootableAt().orElse(Instant.EPOCH);
            if (Instant.now().isBefore(lootableAt)) {
                continue;
            }
            Instant endAt = record.cleanupAt();
            if (endAt == null || !Instant.now().isBefore(endAt)) {
                continue;
            }
            Location anchor = record.anchor();
            if (anchor.getWorld() == null || !anchor.getWorld().equals(player.getWorld())) {
                continue;
            }
            double radius = lifecycle.bossBarRadius;
            if (player.getLocation().distanceSquared(anchor) > radius * radius) {
                continue;
            }
            long remaining = Math.max(0L, Duration.between(Instant.now(), endAt).toSeconds());
            if (best == null || remaining < best.remainingSeconds()) {
                best = new SessionView(
                        sessionId,
                        record,
                        definition.get(),
                        lootableAt,
                        endAt,
                        remaining
                );
            }
        }
        return Optional.ofNullable(best);
    }

    private void updateBar(Player player, SessionView session) {
        LifecycleSettings lifecycle = session.definition().settings().lifecycle;
        long totalSeconds = session.record().looted()
                ? Math.max(1L, lifecycle.cleanupSecondsAfterLooted)
                : Math.max(1L, lifecycle.maxActiveSecondsAfterLootable);
        float progress = Math.max(0F, Math.min(1F, session.remainingSeconds() / (float) totalSeconds));
        int frame = animationFrame();
        boolean looted = session.record().looted();
        long remaining = session.remainingSeconds();

        if (remaining <= 15L) {
            float wobble = (frame % 2 == 0) ? 0.025F : -0.025F;
            progress = Math.max(0F, Math.min(1F, progress + wobble));
        }
        final float barProgress = progress;

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("timer", buildAnimatedTimer(formatTimer(remaining), frame, remaining, looted));
        placeholders.put("pulse", PULSE_SYMBOLS[frame % PULSE_SYMBOLS.length]);
        placeholders.put(
                "type_name",
                messages.resolvePlain(session.definition().settings().displayNameKey, Map.of())
        );
        String messageKey = looted ? lifecycle.bossBarDespawnLootedKey : lifecycle.bossBarDespawnKey;
        Component title = messages.resolve(messageKey, placeholders);
        BossBar.Color color = pickBarColor(remaining, looted, frame);
        BossBar bar = playerBars.computeIfAbsent(player.getUniqueId(), ignored -> BossBar.bossBar(
                title,
                barProgress,
                color,
                BossBar.Overlay.NOTCHED_20
        ));
        bar.name(title);
        bar.progress(barProgress);
        bar.color(color);
        player.showBossBar(bar);
    }

    private static int animationFrame() {
        return (int) ((System.currentTimeMillis() / 400L) % 256);
    }

    private static String buildAnimatedTimer(String timer, int frame, long remainingSeconds, boolean looted) {
        String[] pair;
        if (looted) {
            pair = LOOTED_GRADIENTS[frame % LOOTED_GRADIENTS.length];
        } else if (remainingSeconds <= 30L) {
            pair = CRITICAL_GRADIENTS[frame % CRITICAL_GRADIENTS.length];
        } else if (remainingSeconds <= 90L) {
            pair = CALM_GRADIENTS[(frame + 2) % CALM_GRADIENTS.length];
        } else {
            pair = CALM_GRADIENTS[frame % CALM_GRADIENTS.length];
        }
        return "<gradient:" + pair[0] + ":" + pair[1] + "><bold>" + timer + "</bold></gradient>";
    }

    private static BossBar.Color pickBarColor(long remainingSeconds, boolean looted, int frame) {
        if (looted) {
            return switch (frame % 4) {
                case 0 -> BossBar.Color.YELLOW;
                case 1 -> BossBar.Color.RED;
                case 2 -> BossBar.Color.WHITE;
                default -> BossBar.Color.PURPLE;
            };
        }
        if (remainingSeconds <= 15L) {
            return frame % 2 == 0 ? BossBar.Color.RED : BossBar.Color.WHITE;
        }
        if (remainingSeconds <= 30L) {
            return switch (frame % 3) {
                case 0 -> BossBar.Color.RED;
                case 1 -> BossBar.Color.YELLOW;
                default -> BossBar.Color.WHITE;
            };
        }
        if (remainingSeconds <= 90L) {
            return frame % 2 == 0 ? BossBar.Color.YELLOW : BossBar.Color.GREEN;
        }
        return frame % 2 == 0 ? BossBar.Color.GREEN : BossBar.Color.YELLOW;
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

    private record SessionView(
            UUID sessionId,
            AirDropSessionRegistry.SessionRecord record,
            AirDropTypeDefinition definition,
            Instant lootableAt,
            Instant endAt,
            long remainingSeconds
    ) {
    }
}
