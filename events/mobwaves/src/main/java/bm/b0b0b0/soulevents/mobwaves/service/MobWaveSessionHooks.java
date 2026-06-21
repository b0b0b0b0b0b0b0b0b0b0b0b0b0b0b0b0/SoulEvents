package bm.b0b0b0.soulevents.mobwaves.service;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface MobWaveSessionHooks {

    void onMobKilled(UUID sessionId, Location deathLocation, Player killer, int waveIndex, boolean superBoss);

    default void onBossKilled(UUID sessionId, int waveIndex, Player killer, int sessionKills) {
    }

    void onAllWavesComplete(UUID sessionId);

    default void onWaveTimerExpired(UUID sessionId) {
    }
}
