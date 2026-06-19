package bm.b0b0b0.soulevents.core.schematic;

import bm.b0b0b0.soulevents.core.config.settings.SchematicPlacementSettings;
import bm.b0b0b0.soulevents.core.config.settings.SchematicTerrainMaterialsSettings;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SchematicTerrainAdapter {

    private final SchematicMaterialSet naturalTop;

    private SchematicTerrainAdapter(SchematicMaterialSet naturalTop) {
        this.naturalTop = naturalTop;
    }

    public static SchematicTerrainAdapter from(SchematicTerrainMaterialsSettings settings) {
        return new SchematicTerrainAdapter(SchematicMaterialSet.terrainNaturalTop(settings));
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
        for (SchematicFloorColumn column : SchematicFloorSupport.perimeterFloorColumns(metadata.floorColumns())) {
            int surfaceY = NaturalSurfaceResolver.placementGroundY(
                    world,
                    pasteX + column.dx(),
                    pasteZ + column.dz()
            );
            int targetY = pasteY + column.floorDy();
            if (surfaceY >= targetY) {
                continue;
            }
            if (targetY - surfaceY > limit) {
                return false;
            }
        }
        int approachRing = Math.max(0, placement.terrainApproachRing);
        if (approachRing > 0) {
            for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(
                    metadata.floorColumns(),
                    approachRing
            )) {
                int edgeWorldY = pasteY + column.edgeReferenceDy();
                int naturalY = NaturalSurfaceResolver.placementGroundY(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz()
                );
                float blend = (float) column.ringDistance() / (approachRing + 1);
                int targetWorldY = Math.round(edgeWorldY + (naturalY - edgeWorldY) * blend);
                if (naturalY >= targetWorldY) {
                    continue;
                }
                if (targetWorldY - naturalY > limit) {
                    return false;
                }
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
        int changed = 0;
        for (SchematicFloorColumn column : SchematicFloorSupport.perimeterFloorColumns(metadata.floorColumns())) {
            changed += adaptColumn(
                    world,
                    pasteX + column.dx(),
                    pasteZ + column.dz(),
                    pasteY + column.floorDy(),
                    limit
            );
        }
        int approachRing = Math.max(0, placement.terrainApproachRing);
        if (approachRing > 0) {
            for (SchematicApproachColumn column : SchematicFloorSupport.approachRingColumns(
                    metadata.floorColumns(),
                    approachRing
            )) {
                changed += adaptApproachColumn(
                        world,
                        pasteX + column.dx(),
                        pasteZ + column.dz(),
                        pasteY,
                        column,
                        approachRing,
                        limit
                );
            }
        }
        return changed;
    }

    public int adaptApproachColumn(
            World world,
            int x,
            int z,
            int pasteY,
            SchematicApproachColumn column,
            int ringDepth,
            int limit
    ) {
        int edgeWorldY = pasteY + column.edgeReferenceDy();
        int naturalY = NaturalSurfaceResolver.placementGroundY(world, x, z);
        float blend = (float) column.ringDistance() / (ringDepth + 1);
        int targetWorldY = Math.round(edgeWorldY + (naturalY - edgeWorldY) * blend);
        if (naturalY >= targetWorldY) {
            return 0;
        }
        int delta = targetWorldY - naturalY;
        if (delta > limit) {
            return 0;
        }
        return fillUp(world, x, z, naturalY, targetWorldY);
    }

    public int adaptColumn(World world, int x, int z, int targetFloorY, int limit) {
        int surfaceY = NaturalSurfaceResolver.placementGroundY(world, x, z);
        if (surfaceY >= targetFloorY) {
            return 0;
        }
        int delta = targetFloorY - surfaceY;
        if (delta > limit) {
            return 0;
        }
        return fillUp(world, x, z, surfaceY, targetFloorY);
    }

    private int fillUp(World world, int x, int z, int surfaceY, int targetFloorY) {
        Material top = sampleNaturalTop(world, x, z);
        int changed = 0;
        for (int y = surfaceY + 1; y <= targetFloorY; y++) {
            Material material = y == targetFloorY ? surfaceFillMaterial(top) : Material.DIRT;
            Block block = world.getBlockAt(x, y, z);
            if (block.getType() != material) {
                block.setType(material, false);
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

    Material surfaceFillMaterial(Material sampled) {
        if (naturalTop.contains(Material.GRASS_BLOCK)) {
            return Material.GRASS_BLOCK;
        }
        return topMaterial(sampled);
    }

    int highestSolidY(World world, int x, int z) {
        return NaturalSurfaceResolver.groundY(world, x, z);
    }
}
