package bm.b0b0b0.soulevents.airdrop.service;

import bm.b0b0b0.soulevents.airdrop.chest.AirDropChestHolder;
import bm.b0b0b0.soulevents.airdrop.service.AirDropClusterChestPlacer.ReplacedBlock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AirDropSessionRegistry {

    private final ConcurrentHashMap<UUID, SessionRecord> sessions = new ConcurrentHashMap<>();

    public void register(
            UUID sessionId,
            Location anchor,
            Material chestMaterial,
            Material originalMaterial,
            BlockData originalBlockData,
            boolean clusterEnabled,
            List<ReplacedBlock> clusterBlocks,
            int clusterLootSlotIndex
    ) {
        sessions.put(sessionId, new SessionRecord(
                anchor.clone(),
                chestMaterial,
                originalMaterial,
                originalBlockData,
                clusterEnabled,
                clusterBlocks == null ? List.of() : List.copyOf(clusterBlocks),
                clusterLootSlotIndex,
                null,
                null,
                List.of(),
                null,
                false,
                false
        ));
    }

    public void assignLootChests(UUID sessionId, List<AirDropChestHolder> lootChests) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        sessions.put(sessionId, current.withLootChests(lootChests));
    }

    public Optional<SessionRecord> findByAnchor(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        for (SessionRecord record : sessions.values()) {
            if (matchesAnchor(record, location)) {
                return Optional.of(record);
            }
        }
        return Optional.empty();
    }

    public Optional<SessionRecord> find(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Optional<UUID> sessionIdAt(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        for (var entry : sessions.entrySet()) {
            if (matchesAnchor(entry.getValue(), location)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    private static boolean matchesAnchor(SessionRecord record, Location location) {
        Location anchor = record.anchor();
        if (anchor.getWorld().equals(location.getWorld())
                && anchor.getBlockX() == location.getBlockX()
                && anchor.getBlockY() == location.getBlockY()
                && anchor.getBlockZ() == location.getBlockZ()) {
            return true;
        }
        if (record.clusterEnabled()) {
            return AirDropClusterChestPlacer.isClusterChestLocation(anchor, location);
        }
        return false;
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
        cancelTask(record.unlockBroadcastTask());
        cancelTask(record.cleanupTask());
    }

    public void shutdown() {
        for (UUID sessionId : sessions.keySet().stream().toList()) {
            remove(sessionId);
        }
    }

    private static void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }

    public record SessionRecord(
            Location anchor,
            Material chestMaterial,
            Material originalMaterial,
            BlockData originalBlockData,
            boolean clusterEnabled,
            List<ReplacedBlock> clusterBlocks,
            int clusterLootSlotIndex,
            BukkitTask unlockBroadcastTask,
            BukkitTask cleanupTask,
            List<AirDropChestHolder> lootChests,
            Instant cleanupAt,
            boolean opened,
            boolean looted
    ) {

        public Optional<AirDropChestHolder> lootChest(int clusterIndex) {
            if (lootChests == null || clusterIndex < 0 || clusterIndex >= lootChests.size()) {
                return Optional.empty();
            }
            AirDropChestHolder holder = lootChests.get(clusterIndex);
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
                    anchor,
                    chestMaterial,
                    originalMaterial,
                    originalBlockData,
                    clusterEnabled,
                    clusterBlocks,
                    clusterLootSlotIndex,
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
                    anchor,
                    chestMaterial,
                    originalMaterial,
                    originalBlockData,
                    clusterEnabled,
                    clusterBlocks,
                    clusterLootSlotIndex,
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
                    anchor,
                    chestMaterial,
                    originalMaterial,
                    originalBlockData,
                    clusterEnabled,
                    clusterBlocks,
                    clusterLootSlotIndex,
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
                    anchor,
                    chestMaterial,
                    originalMaterial,
                    originalBlockData,
                    clusterEnabled,
                    clusterBlocks,
                    clusterLootSlotIndex,
                    unlockBroadcastTask,
                    cleanupTask,
                    lootChests == null ? List.of() : List.copyOf(lootChests),
                    cleanupAt,
                    opened,
                    looted
            );
        }

        public boolean isChestIntact() {
            if (anchor.getWorld() == null) {
                return false;
            }
            if (clusterEnabled) {
                return AirDropClusterChestPlacer.areClusterChestsIntact(anchor, chestMaterial);
            }
            return anchor.getBlock().getType() == chestMaterial;
        }

        public boolean isDecoyCluster() {
            return clusterEnabled && clusterLootSlotIndex >= 0;
        }
    }
}
