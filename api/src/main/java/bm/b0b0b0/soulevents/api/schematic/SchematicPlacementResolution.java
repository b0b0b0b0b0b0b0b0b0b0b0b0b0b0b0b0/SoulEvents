package bm.b0b0b0.soulevents.api.schematic;

import org.bukkit.Location;

import java.util.Optional;

public record SchematicPlacementResolution(Optional<Location> location, String rejectionReason) {

    public static SchematicPlacementResolution accepted(Location location) {
        return new SchematicPlacementResolution(Optional.of(location), null);
    }

    public static SchematicPlacementResolution rejected(String reason) {
        return new SchematicPlacementResolution(Optional.empty(), reason);
    }

    public boolean accepted() {
        return location.isPresent();
    }
}
