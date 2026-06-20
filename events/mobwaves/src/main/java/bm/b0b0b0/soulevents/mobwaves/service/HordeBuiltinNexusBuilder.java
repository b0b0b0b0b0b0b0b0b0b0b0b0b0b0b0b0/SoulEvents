package bm.b0b0b0.soulevents.mobwaves.service;

import bm.b0b0b0.soulevents.mobwaves.config.settings.HordeBuiltinNexusSettings;
import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class HordeBuiltinNexusBuilder {

    private HordeBuiltinNexusBuilder() {
    }

    public static Optional<BuiltNexus> build(Location surfaceOrigin, HordeBuiltinNexusSettings settings) {
        World world = surfaceOrigin.getWorld();
        if (world == null || !settings.enabled) {
            return Optional.empty();
        }
        int centerX = surfaceOrigin.getBlockX();
        int centerZ = surfaceOrigin.getBlockZ();
        int surfaceY = surfaceOrigin.getBlockY();
        int radius = Math.max(1, settings.footprintRadius);
        int bury = Math.max(0, settings.buryDepth);
        int visible = Math.max(1, settings.visibleHeight);
        int bottomY = surfaceY - bury;
        int topY = surfaceY + visible;

        Material core = material(settings.coreMaterial, Material.CRYING_OBSIDIAN);
        Material shell = material(settings.shellMaterial, Material.DEEPSLATE_BRICKS);
        Material cap = material(settings.capMaterial, Material.RESPAWN_ANCHOR);

        List<BlockSnapshot> snapshots = new ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                for (int y = bottomY; y <= topY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType().isAir() && y > surfaceY + 1) {
                        continue;
                    }
                    boolean centerColumn = x == centerX && z == centerZ;
                    boolean edge = Math.abs(x - centerX) == radius || Math.abs(z - centerZ) == radius;
                    Material target;
                    if (y == topY && centerColumn) {
                        target = cap;
                    } else if (centerColumn) {
                        target = core;
                    } else if (edge || y <= surfaceY) {
                        target = shell;
                    } else {
                        continue;
                    }
                    snapshots.add(BlockSnapshot.capture(block));
                    block.setType(target, false);
                }
            }
        }
        Location waveAnchor = new Location(world, centerX + 0.5, surfaceY + 1.0, centerZ + 0.5);
        Location visualAnchor = new Location(world, centerX + 0.5, topY + 0.5, centerZ + 0.5);
        return Optional.of(new BuiltNexus(waveAnchor, visualAnchor, snapshots));
    }

    public static void undo(BuiltNexus nexus) {
        for (int index = nexus.snapshots().size() - 1; index >= 0; index--) {
            nexus.snapshots().get(index).restore();
        }
    }

    public static List<FlatSurfaceOffset> footprintOffsets(HordeBuiltinNexusSettings settings) {
        int radius = Math.max(1, settings.footprintRadius);
        List<FlatSurfaceOffset> offsets = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                offsets.add(new FlatSurfaceOffset(x, z));
            }
        }
        return offsets;
    }

    private static Material material(String name, Material fallback) {
        Material matched = Material.matchMaterial(name);
        return matched == null || matched.isAir() ? fallback : matched;
    }

    public record BuiltNexus(Location waveAnchor, Location visualAnchor, List<BlockSnapshot> snapshots) {
    }

    private record BlockSnapshot(World world, int x, int y, int z, Material type, org.bukkit.block.data.BlockData data) {

        static BlockSnapshot capture(Block block) {
            return new BlockSnapshot(
                    block.getWorld(),
                    block.getX(),
                    block.getY(),
                    block.getZ(),
                    block.getType(),
                    block.getBlockData().clone()
            );
        }

        void restore() {
            Block block = world.getBlockAt(x, y, z);
            block.setType(type, false);
            block.setBlockData(data, false);
        }
    }
}
