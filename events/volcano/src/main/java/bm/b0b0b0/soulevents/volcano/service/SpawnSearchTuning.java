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
            int searchTimeoutSeconds,
            boolean bypassPlayerProximity
    ) {
    }

    private SpawnSearchTuning() {
    }

    static Values resolve(String source, RandomSpawnSettings spawn, boolean bypassLimits) {
        int timeout = searchTimeoutSeconds(source, spawn);
        if ("admin".equals(source)) {
            return new Values(
                    Math.max(1, spawn.landProbeSamples),
                    Math.max(24, spawn.landProbeRadius),
                    Math.max(1, spawn.maxAttempts),
                    48,
                    16,
                    5,
                    timeout,
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
                timeout,
                bypassLimits
        );
    }

    static int searchTimeoutSeconds(String source, RandomSpawnSettings spawn) {
        int configured = Math.max(5, spawn.searchTimeoutSeconds);
        if ("admin".equals(source)) {
            return Math.min(configured, 15);
        }
        return Math.min(configured, 20);
    }
}
