package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicSettings;

import java.nio.file.Path;
import java.util.List;

public record SchematicDefinition(
        String id,
        Path directory,
        SchematicSettings settings,
        SchematicMetadata metadata
) {

    public boolean isReady() {
        return metadata != null;
    }

    public record SchematicMetadata(
            int sizeX,
            int sizeY,
            int sizeZ,
            int originX,
            int originY,
            int originZ,
            int regionMinX,
            int regionMinY,
            int regionMinZ,
            int regionMaxX,
            int regionMaxY,
            int regionMaxZ,
            int chestOffsetX,
            int chestOffsetY,
            int chestOffsetZ,
            boolean markerDetected,
            List<FlatSurfaceOffset> footprint,
            List<FlatSurfaceOffset> surfaceProbe,
            int blockCount
    ) {
    }
}
