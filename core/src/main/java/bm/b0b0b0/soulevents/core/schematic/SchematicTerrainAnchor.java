package bm.b0b0b0.soulevents.core.schematic;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SchematicTerrainAnchor {

    private SchematicTerrainAnchor() {
    }

    static Material resolveDominantSurface(
            World world,
            int pasteX,
            int pasteZ,
            List<SchematicFloorColumn> floorColumns
    ) {
        Map<Material, Integer> counts = new HashMap<>();
        if (floorColumns.isEmpty()) {
            sampleColumn(world, pasteX, pasteZ, counts);
        } else {
            for (SchematicFloorColumn column : floorColumns) {
                sampleColumn(world, pasteX + column.dx(), pasteZ + column.dz(), counts);
            }
        }
        return counts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(readSurfaceBlock(world, pasteX, pasteZ));
    }

    static Material subsurfaceFor(Material surface) {
        return switch (surface) {
            case GRASS_BLOCK, PODZOL, MYCELIUM, SNOW_BLOCK -> Material.DIRT;
            default -> surface;
        };
    }

    private static void sampleColumn(World world, int x, int z, Map<Material, Integer> counts) {
        Material type = readSurfaceBlock(world, x, z);
        if (!type.isAir() && type.isSolid()) {
            counts.merge(type, 1, Integer::sum);
        }
    }

    private static Material readSurfaceBlock(World world, int x, int z) {
        int y = NaturalSurfaceResolver.spawnSurfaceY(world, x, z);
        Block block = world.getBlockAt(x, y, z);
        return block.getType();
    }
}
