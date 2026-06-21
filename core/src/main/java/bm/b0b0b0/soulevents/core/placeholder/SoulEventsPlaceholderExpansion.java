package bm.b0b0b0.soulevents.core.placeholder;

import bm.b0b0b0.soulevents.api.stats.EventStatsMetrics;
import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class SoulEventsPlaceholderExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final PlayerEventStatsService stats;

    public SoulEventsPlaceholderExpansion(Plugin plugin, PlayerEventStatsService stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "soulevents";
    }

    @Override
    public @NotNull String getAuthor() {
        return "b0b0b0";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "0";
        }
        stats.ensureReady(player.getUniqueId());
        String normalized = params.toLowerCase(Locale.ROOT);
        if (normalized.equals("total_kills")) {
            return format(stats.cachedGrandTotal(player.getUniqueId(), EventStatsMetrics.MOBS_KILLED));
        }
        if (normalized.equals("total_loot")) {
            return format(stats.cachedGrandTotal(player.getUniqueId(), EventStatsMetrics.LOOT_TAKEN));
        }
        if (normalized.equals("total_chests")) {
            return format(sumChestMetrics(player.getUniqueId()));
        }

        ParsedPlaceholder parsed = ParsedPlaceholder.parse(normalized);
        if (parsed == null) {
            return null;
        }
        if (!EventStatsMetrics.MODULES.contains(parsed.moduleId())) {
            return "0";
        }
        return format(stats.cached(player.getUniqueId(), parsed.moduleId(), parsed.scopeId(), parsed.metric()));
    }

    private long sumChestMetrics(java.util.UUID playerId) {
        long sum = 0L;
        sum += stats.cachedModuleTotal(playerId, EventStatsMetrics.MODULE_AIRDROP, EventStatsMetrics.CHESTS_LOOTED);
        sum += stats.cachedModuleTotal(playerId, EventStatsMetrics.MODULE_AIRDROP, EventStatsMetrics.CHESTS_OPENED);
        return sum;
    }

    private static String format(long value) {
        return Long.toString(Math.max(0L, value));
    }

    private record ParsedPlaceholder(String moduleId, String scopeId, String metric) {

        private static ParsedPlaceholder parse(String params) {
            String[] parts = params.split("_");
            if (parts.length < 2) {
                return null;
            }
            String moduleId = parts[0];
            if ("total".equals(moduleId)) {
                return null;
            }
            String metricToken = parts[1];
            String metric = switch (metricToken) {
                case "kills" -> EventStatsMetrics.MOBS_KILLED;
                case "loot" -> EventStatsMetrics.LOOT_TAKEN;
                case "chests" -> EventStatsMetrics.CHESTS_LOOTED;
                case "opens" -> EventStatsMetrics.CHESTS_OPENED;
                default -> null;
            };
            if (metric == null) {
                return null;
            }
            String scopeId = EventStatsMetrics.SCOPE_GLOBAL;
            if (parts.length >= 3) {
                scopeId = String.join("_", java.util.Arrays.copyOfRange(parts, 2, parts.length));
            }
            return new ParsedPlaceholder(moduleId, EventStatsMetrics.normalizeScope(scopeId), metric);
        }
    }
}
