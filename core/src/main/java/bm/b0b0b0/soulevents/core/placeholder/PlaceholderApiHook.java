package bm.b0b0b0.soulevents.core.placeholder;

import bm.b0b0b0.soulevents.api.stats.PlayerEventStatsService;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class PlaceholderApiHook {

    private PlaceholderApiHook() {
    }

    public static boolean hook(Plugin plugin, PlayerEventStatsService stats) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return false;
        }
        SoulEventsPlaceholderExpansion expansion = new SoulEventsPlaceholderExpansion(plugin, stats);
        if (!expansion.register()) {
            plugin.getLogger().warning("PlaceholderAPI found but SoulEvents expansion failed to register.");
            return false;
        }
        return true;
    }
}
