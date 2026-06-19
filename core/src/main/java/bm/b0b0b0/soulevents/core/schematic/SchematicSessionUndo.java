package bm.b0b0b0.soulevents.core.schematic;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SchematicSessionUndo {

    private final ConcurrentHashMap<UUID, SessionUndo> sessions = new ConcurrentHashMap<>();

    public void store(UUID sessionId, String worldName, List<WorldEditSchematicBridge.BlockSnapshot> snapshots) {
        sessions.put(sessionId, new SessionUndo(worldName, snapshots));
    }

    public SessionUndo remove(UUID sessionId) {
        return sessions.remove(sessionId);
    }

    public void clear() {
        sessions.clear();
    }

    public record SessionUndo(String worldName, List<WorldEditSchematicBridge.BlockSnapshot> snapshots) {
    }
}
