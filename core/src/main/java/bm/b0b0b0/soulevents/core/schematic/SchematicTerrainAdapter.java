package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SchematicTerrainAdapter {

    private final SchematicMaterialSet naturalTop;
    private final SchematicMaterialSet removable;

    private SchematicTerrainAdapter(
            SchematicMaterialSet naturalTop,
            SchematicMaterialSet removable
    ) {
        this.naturalTop = naturalTop;
        this.removable = removable;
    }

    public static SchematicTerrainAdapter from(SchematicTerrainMaterialsSettings settings) {
        return new SchematicTerrainAdapter(
                new SchematicMaterialSet(settings.naturalTop, SchematicTerrainMaterialsSettings.defaultNaturalTop()),
                new SchematicMaterialSet(settings.removable, SchematicTerrainMaterialsSettings.defaultRemovable())
        );
    }

    public boolean canAdaptAll(
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

    public int adapt(
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

    public int adaptColumn(World world, int x, int z, int targetFloorY, int limit) {
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

    private int fillUp(World world, int x, int z, int surfaceY, int targetFloorY) {
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

    private int cutDown(World world, int x, int z, int targetFloorY, int surfaceY) {
        int changed = 0;
        for (int y = surfaceY; y > targetFloorY; y--) {
            Block block = world.getBlockAt(x, y, z);
            if (!removable.contains(block.getType()) && !block.getType().isAir()) {
                continue;
            }
            if (!block.getType().isAir()) {
                block.setType(Material.AIR, false);
                changed++;
            }
        }
        return changed;
    }

    Material sampleNaturalTop(World world, int x, int z) {
        for (int ox = -3; ox <= 3; ox++) {
            for (int oz = -3; oz <= 3; oz++) {
                if (ox == 0 && oz == 0) {
                    continue;
                }
                int y = highestSolidY(world, x + ox, z + oz);
                Material material = world.getBlockAt(x + ox, y, z + oz).getType();
                if (naturalTop.contains(material)) {
                    return material;
                }
            }
        }
        return Material.GRASS_BLOCK;
    }

    Material topMaterial(Material reference) {
        if (reference == Material.DIRT || reference == Material.COARSE_DIRT || reference == Material.ROOTED_DIRT) {
            return Material.GRASS_BLOCK;
        }
        return reference;
    }

    int highestSolidY(World world, int x, int z) {
        return NaturalSurfaceResolver.groundY(world, x, z);
    }
}
