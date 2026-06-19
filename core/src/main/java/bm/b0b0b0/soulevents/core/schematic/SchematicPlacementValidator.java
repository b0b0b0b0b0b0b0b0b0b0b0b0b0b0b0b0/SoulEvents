package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Optional;

public final class SchematicPlacementValidator {

    public Optional<Location> resolve(
            World world,
            int pasteOriginBlockX,
            int pasteOriginBlockZ,
            SchematicDefinition definition
    ) {
        if (!definition.isReady()) {
            return Optional.empty();
        }
        SchematicDefinition.SchematicMetadata metadata = definition.metadata();
        SchematicPlacementSettings placement = definition.settings().placement;
        List<FlatSurfaceOffset> probe = metadata.surfaceProbe();
        if (probe.isEmpty()) {
            return Optional.empty();
        }

        int[] surfaceHeights = new int[probe.size()];
        for (int index = 0; index < probe.size(); index++) {
            FlatSurfaceOffset offset = probe.get(index);
            surfaceHeights[index] = world.getHighestBlockYAt(
                    pasteOriginBlockX + offset.dx(),
                    pasteOriginBlockZ + offset.dz()
            );
        }

        int referenceY = surfaceHeights[0];
        for (int height : surfaceHeights) {
            if (Math.abs(height - referenceY) > placement.maxSurfaceDelta) {
                return Optional.empty();
            }
        }

        for (FlatSurfaceOffset offset : probe) {
            int x = pasteOriginBlockX + offset.dx();
            int z = pasteOriginBlockZ + offset.dz();
            if (!isSurfaceValid(world, x, referenceY, z, placement)) {
                return Optional.empty();
            }
        }

        int pasteY = referenceY + placement.verticalOffset - (metadata.regionMinY() - metadata.originY());
        if (!hasClearance(world, pasteOriginBlockX, pasteY, pasteOriginBlockZ, metadata, placement)) {
            return Optional.empty();
        }

        return Optional.of(new Location(world, pasteOriginBlockX, pasteY, pasteOriginBlockZ));
    }

    private static boolean hasClearance(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        int minDx = metadata.regionMinX() - metadata.originX();
        int maxDx = metadata.regionMaxX() - metadata.originX();
        int minDy = metadata.regionMinY() - metadata.originY();
        int maxDy = metadata.regionMaxY() - metadata.originY();
        int minDz = metadata.regionMinZ() - metadata.originZ();
        int maxDz = metadata.regionMaxZ() - metadata.originZ();

        for (int dx = minDx; dx <= maxDx; dx++) {
            for (int dz = minDz; dz <= maxDz; dz++) {
                for (int dy = minDy + 1; dy <= maxDy; dy++) {
                    Block block = world.getBlockAt(pasteX + dx, pasteY + dy, pasteZ + dz);
                    if (!block.isPassable() && !block.isEmpty()) {
                        return false;
                    }
                }
                for (int air = 0; air < placement.minAirAbove; air++) {
                    Block above = world.getBlockAt(pasteX + dx, pasteY + maxDy + 1 + air, pasteZ + dz);
                    if (!above.isPassable() && !above.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isSurfaceValid(
            World world,
            int x,
            int y,
            int z,
            SchematicPlacementSettings placement
    ) {
        if (world.getHighestBlockYAt(x, z) != y) {
            return false;
        }
        Block surface = world.getBlockAt(x, y, z);
        if (placement.rejectLiquids && (surface.isLiquid() || !surface.isSolid())) {
            return false;
        }
        if (!isValidSurfaceBlock(surface)) {
            return false;
        }
        if (placement.requireSolidBelow) {
            Block below = world.getBlockAt(x, y - 1, z);
            if (below.isLiquid() || !below.isSolid()) {
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
