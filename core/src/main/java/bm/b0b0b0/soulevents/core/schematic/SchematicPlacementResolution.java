package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.Location;

import java.util.Optional;

record SchematicPlacementResolution(Optional<Location> location, String rejectionReason) {

    static SchematicPlacementResolution accepted(Location location) {
        return new SchematicPlacementResolution(Optional.of(location), null);
    }

    static SchematicPlacementResolution rejected(String reason) {
        return new SchematicPlacementResolution(Optional.empty(), reason);
    }

    bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution toApi() {
        return new bm.b0b0b0.soulevents.api.schematic.SchematicPlacementResolution(location, rejectionReason);
    }
}
