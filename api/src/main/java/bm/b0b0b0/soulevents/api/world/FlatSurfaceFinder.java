package bm.b0b0b0.soulevents.api.world;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;
import java.util.Optional;

public interface FlatSurfaceFinder {

    Optional<Location> resolve(
            World world,
            int blockX,
            int blockZ,
            FlatSurfaceRequirements requirements,
            List<FlatSurfaceOffset> footprint
    );

    default int naturalGroundY(World world, int blockX, int blockZ) {
        return world.getHighestBlockYAt(blockX, blockZ);
    }

    default int clearObstructions(World world, int blockX, int blockZ, int minY, int maxY) {
        return 0;
    }
}
