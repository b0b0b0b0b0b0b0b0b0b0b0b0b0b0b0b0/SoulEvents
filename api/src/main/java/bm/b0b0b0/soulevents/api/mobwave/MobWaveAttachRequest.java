package bm.b0b0b0.soulevents.api.mobwave;

import org.bukkit.Location;

import java.util.UUID;

public record MobWaveAttachRequest(
        UUID sessionId,
        String profileId,
        Location anchor,
        int spawnRadius
) {
}
