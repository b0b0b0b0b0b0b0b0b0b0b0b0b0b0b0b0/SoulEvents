package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.EnumSet;
import java.util.Set;

public final class SchematicTerrainAdapter {

    private static final Set<Material> NATURAL_TOP = EnumSet.of(
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.SAND,
            Material.RED_SAND,
            Material.SNOW_BLOCK,
            Material.MOSS_BLOCK
    );

    private static final Set<Material> REMOVABLE = EnumSet.of(
            Material.GRASS_BLOCK,
            Material.PODZOL,
            Material.MYCELIUM,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT,
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.SNOW,
            Material.SNOW_BLOCK,
            Material.MOSS_BLOCK,
            Material.STONE,
            Material.COBBLESTONE,
            Material.ANDESITE,
            Material.DIORITE,
            Material.GRANITE,
            Material.TALL_GRASS,
            Material.SHORT_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY
    );

    private SchematicTerrainAdapter() {
    }

    public static boolean canAdaptAll(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        int limit = Math.max(0, placement.terrainAdaptBlocks);
        if (limit == 0) {
            return true;
        }
        int floorWorldY = pasteY + metadata.regionMinY() - metadata.originY();
        for (FlatSurfaceOffset offset : metadata.footprint()) {
            int surfaceY = highestSolidY(world, pasteX + offset.dx(), pasteZ + offset.dz());
            if (Math.abs(surfaceY - floorWorldY) > limit) {
                return false;
            }
        }
        return true;
    }

    public static int adapt(
            World world,
            int pasteX,
            int pasteY,
            int pasteZ,
            SchematicDefinition.SchematicMetadata metadata,
            SchematicPlacementSettings placement
    ) {
        int limit = Math.max(0, placement.terrainAdaptBlocks);
        if (limit == 0) {
            return 0;
        }
        int floorWorldY = pasteY + metadata.regionMinY() - metadata.originY();
        int changed = 0;
        for (FlatSurfaceOffset offset : metadata.footprint()) {
            changed += adaptColumn(world, pasteX + offset.dx(), pasteZ + offset.dz(), floorWorldY, limit);
        }
        return changed;
    }

    public static int adaptColumn(World world, int x, int z, int targetFloorY, int limit) {
        return adaptColumnInternal(world, x, z, targetFloorY, limit);
    }

    private static int adaptColumnInternal(World world, int x, int z, int targetFloorY, int limit) {
        int surfaceY = highestSolidY(world, x, z);
        int delta = surfaceY - targetFloorY;
        if (delta == 0) {
            return 0;
        }
        if (Math.abs(delta) > limit) {
            return 0;
        }
        if (delta < 0) {
            return fillUp(world, x, z, surfaceY, targetFloorY);
        }
        return cutDown(world, x, z, targetFloorY, surfaceY);
    }

    private static int fillUp(World world, int x, int z, int surfaceY, int targetFloorY) {
        Material top = sampleNaturalTop(world, x, z);
        int changed = 0;
        for (int y = surfaceY + 1; y <= targetFloorY; y++) {
            Material material = y == targetFloorY ? topMaterial(top) : Material.DIRT;
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != material) {
                block.setType(material, false);
                changed++;
            }
        }
        return changed;
    }

    private static int cutDown(World world, int x, int z, int targetFloorY, int surfaceY) {
        int changed = 0;
        for (int y = surfaceY; y > targetFloorY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!REMOVABLE.contains(block.getType()) && !block.getType().isAir()) {
                continue;
            }
            if (!block.getType().isAir()) {
                block.setType(Material.AIR, false);
                changed++;
            }
        }
        return changed;
    }

    static Material sampleNaturalTop(World world, int x, int z) {
        for (int ox = -3; ox <= 3; ox++) {
            for (int oz = -3; oz <= 3; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int y = highestSolidY(world, x + ox, z + oz);
                Material material = world.getBlockAt(x + ox, y, z + oz).getType();
                if (NATURAL_TOP.contains(material)) {
                    return material;
                }
            }
        }
        return Material.GRASS_BLOCK;
    }

    static Material topMaterial(Material reference) {
        if (reference == Material.DIRT || reference == Material.COARSE_DIRT || reference == Material.ROOTED_DIRT) {
            return Material.GRASS_BLOCK;
        }
        return reference;
    }

    static int highestSolidY(World world, int x, int z) {
        return NaturalSurfaceResolver.groundY(world, x, z);
    }
}
