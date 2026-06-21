package bm.b0b0b0.soulevents.api.stats;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerEventStatsService {

    void increment(UUID playerId, String moduleId, String typeId, String metric, long delta);

    void incrementGlobal(UUID playerId, String moduleId, String metric, long delta);

    void recordSession(UUID playerId, UUID sessionId, String metric, long delta);

    long cached(UUID playerId, String moduleId, String scopeId, String metric);

    long cachedModuleTotal(UUID playerId, String moduleId, String metric);

    long cachedGrandTotal(UUID playerId, String metric);

    CompletableFuture<Long> load(UUID playerId, String moduleId, String scopeId, String metric);

    void flushNow();

    void ensureReady(UUID playerId);
}
