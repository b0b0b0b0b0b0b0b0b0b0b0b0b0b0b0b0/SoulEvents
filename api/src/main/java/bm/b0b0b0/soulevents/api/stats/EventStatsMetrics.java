package bm.b0b0b0.soulevents.api.stats;

import java.util.List;
import java.util.Locale;

public final class EventStatsMetrics {

    public static final String SCOPE_GLOBAL = "global";

    public static final String MODULE_MOBWAVES = "mobwaves";
    public static final String MODULE_AIRDROP = "airdrop";
    public static final String MODULE_VOLCANO = "volcano";

    public static final String MOBS_KILLED = "mobs_killed";
    public static final String LOOT_TAKEN = "loot_taken";
    public static final String CHESTS_OPENED = "chests_opened";
    public static final String CHESTS_LOOTED = "chests_looted";

    public static final List<String> MODULES = List.of(MODULE_MOBWAVES, MODULE_AIRDROP, MODULE_VOLCANO);

    private EventStatsMetrics() {
    }

    public static String normalizeScope(String scopeId) {
        if (scopeId == null || scopeId.isBlank()) {
            return SCOPE_GLOBAL;
        }
        return scopeId.trim().toLowerCase(Locale.ROOT);
    }

    public static String normalizeTypeScope(String typeId) {
        return normalizeScope(typeId);
    }
}
