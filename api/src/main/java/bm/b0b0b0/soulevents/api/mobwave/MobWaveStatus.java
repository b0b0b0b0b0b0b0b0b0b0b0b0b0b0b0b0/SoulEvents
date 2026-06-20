package bm.b0b0b0.soulevents.api.mobwave;

public record MobWaveStatus(
        int currentWave,
        int totalWaves,
        int aliveMobs,
        int pendingSpawns,
        MobWaveChestPhase chestPhase
) {
}
