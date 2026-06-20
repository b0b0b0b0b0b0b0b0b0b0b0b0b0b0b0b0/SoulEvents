package bm.b0b0b0.soulevents.mobwaves.service;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public final class MobHordeSessionRegistry {

    private final Map<UUID, SessionRecord> sessions = new ConcurrentHashMap<>();

    public void register(SessionRecord record) {
        sessions.put(record.sessionId(), record);
    }

    public Optional<SessionRecord> find(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public void remove(UUID sessionId) {
        find(sessionId).ifPresent(record -> {
            record.cancelCleanupTask();
            record.cancelActivationTask();
        });
        sessions.remove(sessionId);
    }

    public List<SessionRecord> snapshot() {
        return List.copyOf(sessions.values());
    }

    public Optional<UUID> sessionIdForEntity(UUID entityId) {
        for (SessionRecord record : sessions.values()) {
            for (LootItem item : record.lootItems()) {
                if (item.entityId.equals(entityId)) {
                    return Optional.of(record.sessionId());
                }
            }
        }
        return Optional.empty();
    }

    public void addLootItem(UUID sessionId, LootItem item) {
        SessionRecord record = sessions.get(sessionId);
        if (record != null) {
            record.lootItems().add(item);
        }
    }

    public int nextSlot(UUID sessionId) {
        SessionRecord record = sessions.get(sessionId);
        if (record == null) {
            return 0;
        }
        return record.nextSlotIndex().getAndIncrement();
    }

    public void markLootClaimed(UUID sessionId, UUID entityId) {
        find(sessionId).ifPresent(record -> {
            for (LootItem item : record.lootItems()) {
                if (item.entityId.equals(entityId)) {
                    item.claimed = true;
                }
            }
        });
    }

    public void assignCleanupTask(UUID sessionId, BukkitTask task) {
        SessionRecord record = sessions.get(sessionId);
        if (record != null) {
            record.assignCleanupTask(task);
        }
    }

    public void assignActivationTask(UUID sessionId, BukkitTask task) {
        SessionRecord record = sessions.get(sessionId);
        if (record != null) {
            record.assignActivationTask(task);
        }
    }

    public static final class SessionRecord {
        private final UUID sessionId;
        private final String typeId;
        private final Location anchor;
        private final Location waveAnchor;
        private final HordeBuiltinNexusBuilder.BuiltNexus builtNexus;
        private final boolean schematicPasted;
        private Instant endAt;
        private Instant expireAt;
        private final AtomicInteger nextSlotIndex = new AtomicInteger(0);
        private final CopyOnWriteArrayList<LootItem> lootItems = new CopyOnWriteArrayList<>();
        private BukkitTask cleanupTask;
        private BukkitTask activationTask;
        private boolean wavesAttached;

        public SessionRecord(
                UUID sessionId,
                String typeId,
                Location anchor,
                Location waveAnchor,
                HordeBuiltinNexusBuilder.BuiltNexus builtNexus,
                boolean schematicPasted
        ) {
            this.sessionId = sessionId;
            this.typeId = typeId;
            this.anchor = anchor;
            this.waveAnchor = waveAnchor;
            this.builtNexus = builtNexus;
            this.schematicPasted = schematicPasted;
        }

        public UUID sessionId() {
            return sessionId;
        }

        public String typeId() {
            return typeId;
        }

        public Location anchor() {
            return anchor;
        }

        public Location waveAnchor() {
            return waveAnchor;
        }

        public HordeBuiltinNexusBuilder.BuiltNexus builtNexus() {
            return builtNexus;
        }

        public boolean schematicPasted() {
            return schematicPasted;
        }

        public Instant endAt() {
            return endAt;
        }

        public void endAt(Instant endAt) {
            this.endAt = endAt;
        }

        public Instant expireAt() {
            return expireAt;
        }

        public void expireAt(Instant expireAt) {
            this.expireAt = expireAt;
        }

        public AtomicInteger nextSlotIndex() {
            return nextSlotIndex;
        }

        public CopyOnWriteArrayList<LootItem> lootItems() {
            return lootItems;
        }

        public void assignCleanupTask(BukkitTask task) {
            cancelCleanupTask();
            cleanupTask = task;
        }

        public void cancelCleanupTask() {
            if (cleanupTask != null) {
                cleanupTask.cancel();
                cleanupTask = null;
            }
        }

        public boolean wavesAttached() {
            return wavesAttached;
        }

        public void markWavesAttached() {
            wavesAttached = true;
        }

        public void assignActivationTask(BukkitTask task) {
            cancelActivationTask();
            activationTask = task;
        }

        public void cancelActivationTask() {
            if (activationTask != null) {
                activationTask.cancel();
                activationTask = null;
            }
        }
    }

    public static final class LootItem {
        public final UUID entityId;
        public final UUID labelId;
        public final int slotIndex;
        public boolean claimed;
        public final int pickableAfterTick;

        public LootItem(UUID entityId, UUID labelId, int slotIndex, int pickableAfterTick) {
            this.entityId = entityId;
            this.labelId = labelId;
            this.slotIndex = slotIndex;
            this.pickableAfterTick = pickableAfterTick;
        }
    }
}
