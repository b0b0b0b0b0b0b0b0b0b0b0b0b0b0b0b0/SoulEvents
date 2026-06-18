package bm.b0b0b0.soulevents.api.module;

import java.time.Duration;

public record EventTypeDefinition(
        String id,
        String displayNameKey,
        Duration defaultInterval,
        boolean playerSummonAllowed
) {
}
