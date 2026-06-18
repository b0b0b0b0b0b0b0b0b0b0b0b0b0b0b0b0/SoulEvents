package bm.b0b0b0.soulevents.api.world;

import java.util.Optional;

public record WorldPlacementResult(
        boolean allowed,
        WorldPlacementDenial denial,
        String worldName,
        String regionName
) {

    public static WorldPlacementResult allow() {
        return new WorldPlacementResult(true, WorldPlacementDenial.NONE, "", "");
    }

    public static WorldPlacementResult deny(
            WorldPlacementDenial denial,
            String worldName,
            String regionName
    ) {
        return new WorldPlacementResult(false, denial, worldName == null ? "" : worldName, regionName == null ? "" : regionName);
    }

    public Optional<String> messageKey() {
        if (allowed) {
            return Optional.empty();
        }
        return Optional.of(denial.messageKey());
    }
}
