package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.api.world.FlatSurfaceOffset;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class AirDropClusterChestPlacer {

    private static final ClusterSlot[] SLOTS = {
            new ClusterSlot(0, -1, BlockFace.NORTH),
            new ClusterSlot(0, 1, BlockFace.SOUTH),
            new ClusterSlot(1, 0, BlockFace.EAST),
            new ClusterSlot(-1, 0, BlockFace.WEST)
    };

    private AirDropClusterChestPlacer() {
    }

    public record ReplacedBlock(Location location, Material material, BlockData blockData) {
    }

    public record ClusterPlacement(List<ReplacedBlock> replacedBlocks, int lootSlotIndex) {
    }

    public static final int CLUSTER_CHEST_COUNT = SLOTS.length;

    public static List<FlatSurfaceOffset> footprintOffsets() {
        List<FlatSurfaceOffset> footprint = new ArrayList<>(SLOTS.length + 1);
        footprint.add(new FlatSurfaceOffset(0, 0));
        for (ClusterSlot slot : SLOTS) {
            footprint.add(new FlatSurfaceOffset(slot.offsetX(), slot.offsetZ()));
        }
        return List.copyOf(footprint);
    }

    public static ClusterPlacement install(
            Location center,
            Material chestMaterial,
            boolean decoyMode,
            int fixedLootSlot
    ) {
        List<ReplacedBlock> replaced = new ArrayList<>(SLOTS.length);
        int lootSlot = -1;
        if (decoyMode) {
            lootSlot = fixedLootSlot >= 0 ? fixedLootSlot : ThreadLocalRandom.current().nextInt(SLOTS.length);
        }
        for (ClusterSlot slot : SLOTS) {
            Location target = center.clone().add(slot.offsetX(), 0, slot.offsetZ());
            replaceWithChest(replaced, target, chestMaterial, slot.face());
        }
        return new ClusterPlacement(List.copyOf(replaced), lootSlot);
    }

    public static ClusterPlacement install(Location center, Material chestMaterial, boolean decoyMode) {
        return install(center, chestMaterial, decoyMode, -1);
    }

    public static void restore(List<ReplacedBlock> replacedBlocks) {
        if (replacedBlocks == null || replacedBlocks.isEmpty()) {
            return;
        }
        for (ReplacedBlock replaced : replacedBlocks) {
            Block block = replaced.location().getBlock();
            block.setType(replaced.material());
            if (replaced.blockData() != null) {
                block.setBlockData(replaced.blockData());
            }
        }
    }

    public static Optional<Integer> slotIndexAt(Location center, Location clicked) {
        if (center.getWorld() == null || clicked.getWorld() == null) {
            return Optional.empty();
        }
        if (!center.getWorld().equals(clicked.getWorld()) || center.getBlockY() != clicked.getBlockY()) {
            return Optional.empty();
        }
        int dx = clicked.getBlockX() - center.getBlockX();
        int dz = clicked.getBlockZ() - center.getBlockZ();
        for (int index = 0; index < SLOTS.length; index++) {
            ClusterSlot slot = SLOTS[index];
            if (slot.offsetX() == dx && slot.offsetZ() == dz) {
                return Optional.of(index);
            }
        }
        return Optional.empty();
    }

    public static boolean isClusterChestLocation(Location center, Location block) {
        return slotIndexAt(center, block).isPresent();
    }

    public static boolean areClusterChestsIntact(Location center, Material chestMaterial) {
        for (ClusterSlot slot : SLOTS) {
            Block block = center.clone().add(slot.offsetX(), 0, slot.offsetZ()).getBlock();
            if (block.getType() != chestMaterial) {
                return false;
            }
        }
        return true;
    }

    private static void replaceWithChest(
            List<ReplacedBlock> replaced,
            Location location,
            Material material,
            BlockFace face
    ) {
        Block block = location.getBlock();
        replaced.add(new ReplacedBlock(
                location.clone(),
                block.getType(),
                block.getBlockData().clone()
        ));
        block.setType(material);
        BlockData data = block.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(face);
            block.setBlockData(directional);
        }
    }

    private record ClusterSlot(int offsetX, int offsetZ, BlockFace face) {
    }
}
