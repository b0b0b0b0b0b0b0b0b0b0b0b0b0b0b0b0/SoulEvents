package bm.b0b0b0.soulevents.core.stats;

import java.util.UUID;

record PlayerStatKey(UUID playerId, String moduleId, String scopeId, String metric) {
}
