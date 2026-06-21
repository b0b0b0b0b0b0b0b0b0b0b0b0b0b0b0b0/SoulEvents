package bm.b0b0b0.soulevents.core.listener;

import bm.b0b0b0.soulevents.core.stats.PlayerEventStatsServiceImpl;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerStatsWarmListener implements Listener {

    private final PlayerEventStatsServiceImpl stats;

    public PlayerStatsWarmListener(PlayerEventStatsServiceImpl stats) {
        this.stats = stats;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        stats.warmPlayer(event.getPlayer().getUniqueId());
    }
}
