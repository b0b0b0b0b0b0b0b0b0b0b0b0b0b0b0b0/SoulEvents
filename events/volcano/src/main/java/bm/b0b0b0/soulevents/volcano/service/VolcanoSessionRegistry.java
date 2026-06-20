package bm.b0b0b0.soulevents.volcano.service;

import bm.b0b0b0.soulevents.api.schematic.SchematicWorldBounds;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VolcanoSessionRegistry {

    private final ConcurrentHashMap<UUID, SessionRecord> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID> entityIndex = new ConcurrentHashMap<>();

    public void register(
            UUID sessionId,
            Location ventAnchor,
            Location pasteOrigin,
            Instant eruptAt,
            Instant extinguishAt,
            Optional<SchematicWorldBounds> schematicBounds
    ) {
        sessions.put(sessionId, new SessionRecord(
                ventAnchor,
                pasteOrigin,
                eruptAt,
                extinguishAt,
                false,
                new CopyOnWriteArrayList<>(),
                schematicBounds,
                null,
                null
        ));
    }

    public Optional<SessionRecord> find(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Map<UUID, SessionRecord> snapshot() {
        return Map.copyOf(sessions);
    }

    public Optional<UUID> sessionIdForEntity(UUID entityId) {
        return Optional.ofNullable(entityIndex.get(entityId));
    }

    public void markErupted(UUID sessionId) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        sessions.put(sessionId, current.withErupted(true));
    }

    public void addLootItem(UUID sessionId, LootItem item) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        List<LootItem> items = new CopyOnWriteArrayList<>(current.lootItems());
        items.add(item);
        sessions.put(sessionId, current.withLootItems(items));
        entityIndex.put(item.entityId(), sessionId);
        if (item.labelId() != null) {
            entityIndex.put(item.labelId(), sessionId);
        }
    }

    public void markLootClaimed(UUID sessionId, UUID entityId) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        List<LootItem> updated = current.lootItems().stream()
                .map(item -> item.entityId().equals(entityId) ? item.withClaimed(true) : item)
                .toList();
        sessions.put(sessionId, current.withLootItems(updated));
        entityIndex.remove(entityId);
    }

    public void assignEffectsTask(UUID sessionId, BukkitTask task) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        if (current.effectsTask() != null) {
            current.effectsTask().cancel();
        }
        sessions.put(sessionId, current.withEffectsTask(task));
    }

    public void assignCleanupTask(UUID sessionId, BukkitTask task) {
        SessionRecord current = sessions.get(sessionId);
        if (current == null) {
            return;
        }
        if (current.cleanupTask() != null) {
            current.cleanupTask().cancel();
        }
        sessions.put(sessionId, current.withCleanupTask(task));
    }

    public void remove(UUID sessionId) {
        SessionRecord record = sessions.remove(sessionId);
        if (record == null) {
            return;
        }
        if (record.effectsTask() != null) {
            record.effectsTask().cancel();
        }
        if (record.cleanupTask() != null) {
            record.cleanupTask().cancel();
        }
        for (LootItem item : record.lootItems()) {
            entityIndex.remove(item.entityId());
            if (item.labelId() != null) {
                entityIndex.remove(item.labelId());
            }
        }
    }

    public void shutdown() {
        for (UUID sessionId : List.copyOf(sessions.keySet())) {
            remove(sessionId);
        }
        entityIndex.clear();
    }

    public record LootItem(UUID entityId, UUID labelId, int slotIndex, boolean claimed, int pickableAfterTick) {

        public LootItem withClaimed(boolean claimed) {
            return new LootItem(entityId, labelId, slotIndex, claimed, pickableAfterTick);
        }
    }

    public record SessionRecord(
            Location ventAnchor,
            Location pasteOrigin,
            Instant eruptAt,
            Instant extinguishAt,
            boolean erupted,
            List<LootItem> lootItems,
            Optional<SchematicWorldBounds> schematicBounds,
            BukkitTask effectsTask,
            BukkitTask cleanupTask
    ) {

        public Location anchor() {
            return ventAnchor;
        }

        public SessionRecord withErupted(boolean erupted) {
            return new SessionRecord(
                    ventAnchor, pasteOrigin, eruptAt, extinguishAt, erupted,
                    lootItems, schematicBounds, effectsTask, cleanupTask
            );
        }

        public SessionRecord withLootItems(List<LootItem> lootItems) {
            return new SessionRecord(
                    ventAnchor, pasteOrigin, eruptAt, extinguishAt, erupted,
                    lootItems, schematicBounds, effectsTask, cleanupTask
            );
        }

        public SessionRecord withEffectsTask(BukkitTask effectsTask) {
            return new SessionRecord(
                    ventAnchor, pasteOrigin, eruptAt, extinguishAt, erupted,
                    lootItems, schematicBounds, effectsTask, cleanupTask
            );
        }

        public SessionRecord withCleanupTask(BukkitTask cleanupTask) {
            return new SessionRecord(
                    ventAnchor, pasteOrigin, eruptAt, extinguishAt, erupted,
                    lootItems, schematicBounds, effectsTask, cleanupTask
            );
        }
    }
}
