package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.chest.AirDropChestHolder;
import bm.b0b0b0.soulevents.airdrop.service.AirDropClusterChestPlacer.ReplacedBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AirDropSessionRegistry {

    public static final int LOOT_POINT_STRIDE = 10;

    private final ConcurrentHashMap<UUID, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BlockKey, UUID> blockIndex = new ConcurrentHashMap<>();

    public void register(UUID sessionId, List<LootPoint> lootPoints, Material chestMaterial) {
        SessionRecord record = new SessionRecord(
                lootPoints == null ? List.of() : List.copyOf(lootPoints),
                chestMaterial,
                null,
                null,
                List.of(),
                null,
                false,
                false
        );
        sessions.put(sessionId, record);
        indexSession(sessionId, record);
    }

    public void assignLootChests(UUID sessionId, List<AirDropChestHolder> lootChests) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        sessions.put(sessionId, current.withLootChests(lootChests));
    }

    public Optional<SessionRecord> findByAnchor(Location location) {
        return sessionIdAt(location).flatMap(this::find);
    }

    public Optional<SessionRecord> find(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Optional<UUID> sessionIdAt(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(blockIndex.get(BlockKey.of(location)));
    }

    public static int flatChestIndex(int pointIndex, int clusterIndex) {
        return pointIndex * LOOT_POINT_STRIDE + clusterIndex;
    }

    public static int sequentialChestIndex(List<LootPoint> lootPoints, int pointIndex, int clusterIndex) {
        int index = 0;
        for (int currentPoint = 0; currentPoint < pointIndex; currentPoint++) {
            LootPoint point = lootPoints.get(currentPoint);
            index += point.clusterEnabled() ? AirDropClusterChestPlacer.CLUSTER_CHEST_COUNT : 1;
        }
        return index + clusterIndex;
    }

    public static int totalChestSlots(List<LootPoint> lootPoints) {
        int total = 0;
        for (LootPoint point : lootPoints) {
            total += point.clusterEnabled() ? AirDropClusterChestPlacer.CLUSTER_CHEST_COUNT : 1;
        }
        return total;
    }

    private void indexSession(UUID sessionId, SessionRecord record) {
        if (record == null) {
            return;
        }
        for (BlockKey key : blockKeysFor(record)) {
            blockIndex.put(key, sessionId);
        }
    }

    private void deindexSession(SessionRecord record) {
        if (record == null) {
            return;
        }
        for (BlockKey key : blockKeysFor(record)) {
            blockIndex.remove(key);
        }
    }

    private static List<BlockKey> blockKeysFor(SessionRecord record) {
        List<BlockKey> keys = new ArrayList<>();
        for (LootPoint point : record.lootPoints()) {
            keys.add(BlockKey.of(point.anchor()));
            if (point.clusterEnabled()) {
                for (ReplacedBlock block : point.clusterBlocks()) {
                    keys.add(BlockKey.of(block.location()));
                }
            }
        }
        return keys;
    }

    private record BlockKey(String world, int x, int y, int z) {

        private static BlockKey of(Location location) {
            return new BlockKey(
                    location.getWorld().getName(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }
    }

    public void assignUnlockTask(UUID sessionId, BukkitTask unlockTask) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        if (current.unlockBroadcastTask() != null) {
            current.unlockBroadcastTask().cancel();
        }
        sessions.put(sessionId, current.withUnlockBroadcastTask(unlockTask));
    }

    public void markOpened(UUID sessionId) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null || current.opened()) {
            return;
        }
        sessions.put(sessionId, current.withOpened());
    }

    public void rescheduleCleanup(UUID sessionId, BukkitTask cleanupTask, Instant cleanupAt) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        sessions.put(sessionId, current.withCleanupTask(cleanupTask, cleanupAt));
    }

    public void markLooted(UUID sessionId) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        sessions.put(sessionId, current.withLooted(true));
    }

    public void remove(UUID sessionId) {
        SessionRecord record = sessions.remove(sessionId);
        if (record == null) {
            return;
        }
        deindexSession(record);
        cancelTask(record.unlockBroadcastTask());
        cancelTask(record.cleanupTask());
    }

    public void shutdown() {
        for (UUID sessionId : sessions.keySet().stream().toList()) {
            remove(sessionId);
        }
    }

    public Map<UUID, SessionRecord> snapshot() {
        return Map.copyOf(sessions);
    }

    private static void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public record LootPoint(
            Location anchor,
            Material originalMaterial,
            BlockData originalBlockData,
            boolean clusterEnabled,
            List<ReplacedBlock> clusterBlocks,
            int clusterLootSlotIndex
    ) {

        public boolean isDecoyCluster() {
            return clusterEnabled && clusterLootSlotIndex >= 0;
        }

        public boolean isChestIntact(Material chestMaterial) {
            if (anchor.getWorld() == null) {
                return false;
            }
            if (clusterEnabled) {
                return AirDropClusterChestPlacer.areClusterChestsIntact(anchor, chestMaterial);
            }
            return anchor.getBlock().getType() == chestMaterial;
        }
    }

    public record ChestClickTarget(int pointIndex, int clusterIndex) {
    }

    public record SessionRecord(
            List<LootPoint> lootPoints,
            Material chestMaterial,
            BukkitTask unlockBroadcastTask,
            BukkitTask cleanupTask,
            List<AirDropChestHolder> lootChests,
            Instant cleanupAt,
            boolean opened,
            boolean looted
    ) {

        public Location anchor() {
            return lootPoints.isEmpty() ? null : lootPoints.getFirst().anchor();
        }

        public boolean clusterEnabled() {
            return lootPoints.size() == 1 && lootPoints.getFirst().clusterEnabled();
        }

        public List<ReplacedBlock> clusterBlocks() {
            return lootPoints.size() == 1 ? lootPoints.getFirst().clusterBlocks() : List.of();
        }

        public int clusterLootSlotIndex() {
            return lootPoints.size() == 1 ? lootPoints.getFirst().clusterLootSlotIndex() : -1;
        }

        public Material originalMaterial() {
            return lootPoints.isEmpty() ? Material.AIR : lootPoints.getFirst().originalMaterial();
        }

        public BlockData originalBlockData() {
            return lootPoints.isEmpty() ? null : lootPoints.getFirst().originalBlockData();
        }

        public Optional<ChestClickTarget> resolveClick(Location clickedBlock) {
            for (int pointIndex = 0; pointIndex < lootPoints.size(); pointIndex++) {
                LootPoint point = lootPoints.get(pointIndex);
                if (!point.clusterEnabled()) {
                    if (sameBlock(point.anchor(), clickedBlock)) {
                        return Optional.of(new ChestClickTarget(pointIndex, 0));
                    }
                    continue;
                }
                Optional<Integer> slotIndex = AirDropClusterChestPlacer.slotIndexAt(point.anchor(), clickedBlock);
                if (slotIndex.isPresent()) {
                    return Optional.of(new ChestClickTarget(pointIndex, slotIndex.get()));
                }
            }
            return Optional.empty();
        }

        public Optional<AirDropChestHolder> lootChest(int flatIndex) {
            if (lootChests == null || flatIndex < 0 || flatIndex >= lootChests.size()) {
                return Optional.empty();
            }
            AirDropChestHolder holder = lootChests.get(flatIndex);
            if (holder == null) {
                return Optional.empty();
            }
            return Optional.of(holder);
        }

        public boolean hasLootChests() {
            return lootChests != null && !lootChests.isEmpty();
        }

        public SessionRecord withLootChests(List<AirDropChestHolder> lootChests) {
            return copy(lootChests, cleanupAt, opened, looted);
        }

        public SessionRecord withUnlockBroadcastTask(BukkitTask unlockBroadcastTask) {
            return new SessionRecord(
                    lootPoints,
                    chestMaterial,
                    unlockBroadcastTask,
                    cleanupTask,
                    lootChests,
                    cleanupAt,
                    opened,
                    looted
            );
        }

        public SessionRecord withCleanupTask(BukkitTask cleanupTask, Instant cleanupAt) {
            cancelTask(this.cleanupTask);
            return new SessionRecord(
                    lootPoints,
                    chestMaterial,
                    unlockBroadcastTask,
                    cleanupTask,
                    lootChests,
                    cleanupAt,
                    opened,
                    looted
            );
        }

        public SessionRecord withOpened() {
            return new SessionRecord(
                    lootPoints,
                    chestMaterial,
                    unlockBroadcastTask,
                    cleanupTask,
                    lootChests,
                    cleanupAt,
                    true,
                    looted
            );
        }

        public SessionRecord withLooted(boolean looted) {
            return copy(lootChests, cleanupAt, opened, looted);
        }

        private SessionRecord copy(
                List<AirDropChestHolder> lootChests,
                Instant cleanupAt,
                boolean opened,
                boolean looted
        ) {
            return new SessionRecord(
                    lootPoints,
                    chestMaterial,
                    unlockBroadcastTask,
                    cleanupTask,
                    lootChests == null ? List.of() : List.copyOf(lootChests),
                    cleanupAt,
                    opened,
                    looted
            );
        }

        public boolean isChestIntact() {
            for (LootPoint point : lootPoints) {
                if (!point.isChestIntact(chestMaterial)) {
                    return false;
                }
            }
            return !lootPoints.isEmpty();
        }

        public boolean isDecoyCluster() {
            return lootPoints.size() == 1 && lootPoints.getFirst().isDecoyCluster();
        }

        private static boolean sameBlock(Location left, Location right) {
            return left.getBlockX() == right.getBlockX()
                    && left.getBlockY() == right.getBlockY()
                    && left.getBlockZ() == right.getBlockZ()
                    && left.getWorld() != null
                    && left.getWorld().equals(right.getWorld());
        }
    }
}
