package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.volcano.config.settings.RandomSpawnSettings;

final class SpawnSearchTuning {

    record Values(
            int probeSamples,
            int probeRadius,
            int maxAttempts,
            int scanRadius,
            int scanStep,
            int maxScanCandidates,
            boolean bypassPlayerProximity
    ) {
    }

    private SpawnSearchTuning() {
    }

    static Values resolve(String source, RandomSpawnSettings spawn, boolean bypassLimits) {
        if ("admin".equals(source)) {
            return new Values(
                    4,
                    32,
                    Math.max(1, spawn.maxAttempts),
                    80,
                    16,
                    12,
                    true
            );
        }
        return new Values(
                Math.max(1, spawn.landProbeSamples),
                Math.max(0, spawn.landProbeRadius),
                Math.max(1, spawn.maxAttempts),
                Math.max(32, spawn.landProbeRadius),
                16,
                6,
                bypassLimits
        );
    }
}
