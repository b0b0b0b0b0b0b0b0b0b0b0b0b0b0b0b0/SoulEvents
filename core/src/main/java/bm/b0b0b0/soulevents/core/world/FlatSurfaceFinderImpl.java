package bm.b0b0b0.soulevents.core.world;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceFinder;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceRequirements;
import org.bukkit.Location;
import org.bukkit.Material;
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
            surfaceHeights[index] = world.getHighestBlockYAt(blockX + offset.dx(), blockZ + offset.dz());
        }
        int referenceY = surfaceHeights[0];
        for (int height : surfaceHeights) {
            if (Math.abs(height - referenceY) > requirements.maxSurfaceDelta()) {
                return Optional.empty();
            }
        }
        for (FlatSurfaceOffset offset : points) {
            if (!isPlacementValid(world, blockX + offset.dx(), referenceY, blockZ + offset.dz(), requirements)) {
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

    private static boolean isPlacementValid(
            World world,
            int x,
            int y,
            int z,
            FlatSurfaceRequirements requirements
    ) {
        if (world.getHighestBlockYAt(x, z) != y) {
            return false;
        }
        Block surface = world.getBlockAt(x, y, z);
        if (!isValidSurfaceBlock(surface)) {
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
            if (!above.isPassable() && !above.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidSurfaceBlock(Block block) {
        if (block.isLiquid() || !block.isSolid()) {
            return false;
        }
        Material type = block.getType();
        return type != Material.MAGMA_BLOCK
                && type != Material.CACTUS
                && type != Material.SWEET_BERRY_BUSH
                && type != Material.POWDER_SNOW;
    }
}
