package bm.b0b0b0.soulevents.mobwaves.service;

import org.bukkit.Location;

import java.util.UUID;

public interface MobWaveSessionHooks {

    void onMobKilled(UUID sessionId, Location deathLocation);

    void onAllWavesComplete(UUID sessionId);
}
