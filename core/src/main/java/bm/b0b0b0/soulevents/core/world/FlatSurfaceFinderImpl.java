package bm.b0b0b0.soulevents.core.world;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceRequirements;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;

public final class FlatSurfaceFinderImpl implements FlatSurfaceFinder {

    @Override
    public Optional<Location> resolve(
            World world,
            int blockX,
            int blockZ,
            FlatSurfaceRequirements requirements,
            List<FlatSurfaceOffset> footprint
    ) {
        List<FlatSurfaceOffset> points = footprint == null || footprint.isEmpty()
                ? List.of(new FlatSurfaceOffset(0, 0))
                : footprint;
        int[] surfaceHeights = new int[points.size()];
        for (int index = 0; index < points.size(); index++) {
            FlatSurfaceOffset offset = points.get(index);
            surfaceHeights[index] = naturalGroundY(world, blockX + offset.dx(), blockZ + offset.dz());
        }
        int referenceY = surfaceHeights[0];
        for (int height : surfaceHeights) {
            referenceY = Math.max(referenceY, height);
        }
        for (int height : surfaceHeights) {
            if (referenceY - height > requirements.maxSurfaceDelta()) {
                return Optional.empty();
            }
        }
        for (int index = 0; index < points.size(); index++) {
            FlatSurfaceOffset offset = points.get(index);
            if (!isPlacementValid(
                    world,
                    blockX + offset.dx(),
                    surfaceHeights[index],
                    blockZ + offset.dz(),
                    requirements
            )) {
                return Optional.empty();
            }
        }
        return Optional.of(new Location(
                world,
                blockX + 0.5,
                referenceY + requirements.surfaceYOffset(),
                blockZ + 0.5
        ));
    }

    @Override
    public int naturalGroundY(World world, int blockX, int blockZ) {
        return NaturalSurfaceSupport.naturalGroundY(world, blockX, blockZ);
    }

    @Override
    public int clearObstructions(World world, int blockX, int blockZ, int minY, int maxY) {
        return NaturalSurfaceSupport.clearObstructions(world, blockX, blockZ, minY, maxY);
    }

    private static boolean isPlacementValid(
            World world,
            int x,
            int y,
            int z,
            FlatSurfaceRequirements requirements
    ) {
        Block surface = world.getBlockAt(x, y, z);
        if (!NaturalSurfaceSupport.isValidGroundSurface(surface)) {
            return false;
        }
        if (requirements.requireSolidBelow()) {
            Block below = world.getBlockAt(x, y - 1, z);
            if (below.isLiquid() || !below.isSolid()) {
                return false;
            }
        }
        for (int offsetY = 1; offsetY <= requirements.minAirAbove(); offsetY++) {
            Block above = world.getBlockAt(x, y + offsetY, z);
            if (NaturalSurfaceSupport.isBlockingAbove(above)) {
                return false;
            }
        }
        return true;
    }
}
