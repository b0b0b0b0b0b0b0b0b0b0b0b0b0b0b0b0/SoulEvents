package bm.b0b0b0.soulevents.core.module;

import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventPhase;
import bm.b0b0b0.soulevents.api.module.EventSessionController;
import org.bukkit.Location;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EventSessionControllerImpl implements EventSessionController {

    private final Map<UUID, MutableActiveEvent> sessions = new LinkedHashMap<>();

    @Override
    public ActiveEvent start(String moduleId, String typeId, Location anchor) {
        UUID sessionId = UUID.randomUUID();
        MutableActiveEvent event = new MutableActiveEvent(
                sessionId,
                moduleId,
                typeId,
                EventPhase.PREPARING,
                anchor.clone(),
                Instant.now(),
                Optional.empty()
        );
        sessions.put(sessionId, event);
        return event;
    }

    @Override
    public void setPhase(UUID sessionId, EventPhase phase) {
        MutableActiveEvent event = sessions.get(sessionId);
        if (event != null) {
            event.phase = phase;
        }
    }

    @Override
    public void setLootableAt(UUID sessionId, Instant lootableAt) {
        MutableActiveEvent event = sessions.get(sessionId);
        if (event != null) {
            event.lootableAt = Optional.of(lootableAt);
        }
    }

    @Override
    public void end(UUID sessionId) {
        MutableActiveEvent event = sessions.get(sessionId);
        if (event != null) {
            event.phase = EventPhase.ENDED;
            sessions.remove(sessionId);
        }
    }

    @Override
    public Optional<ActiveEvent> get(UUID sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    public Collection<ActiveEvent> all() {
        return sessions.values().stream().map(MutableActiveEvent::snapshot).toList();
    }

    public void clear() {
        sessions.clear();
    }

    private static final class MutableActiveEvent implements ActiveEvent {

        private final UUID sessionId;
        private final String moduleId;
        private final String typeId;
        private EventPhase phase;
        private final Location anchor;
        private final Instant startedAt;
        private Optional<Instant> lootableAt;

        private MutableActiveEvent(
                UUID sessionId,
                String moduleId,
                String typeId,
                EventPhase phase,
                Location anchor,
                Instant startedAt,
                Optional<Instant> lootableAt
        ) {
            this.sessionId = sessionId;
            this.moduleId = moduleId;
            this.typeId = typeId;
            this.phase = phase;
            this.anchor = anchor;
            this.startedAt = startedAt;
            this.lootableAt = lootableAt;
        }

        private ActiveEvent snapshot() {
            return new ActiveEventSnapshot(
                    sessionId,
                    moduleId,
                    typeId,
                    phase,
                    anchor.clone(),
                    startedAt,
                    lootableAt
            );
        }

        @Override
        public UUID sessionId() {
            return sessionId;
        }

        @Override
        public String moduleId() {
            return moduleId;
        }

        @Override
        public String typeId() {
            return typeId;
        }

        @Override
        public EventPhase phase() {
            return phase;
        }

        @Override
        public Location anchor() {
            return anchor.clone();
        }

        @Override
        public Instant startedAt() {
            return startedAt;
        }

        @Override
        public Optional<Instant> lootableAt() {
            return lootableAt;
        }
    }

    private record ActiveEventSnapshot(
            UUID sessionId,
            String moduleId,
            String typeId,
            EventPhase phase,
            Location anchor,
            Instant startedAt,
            Optional<Instant> lootableAt
    ) implements ActiveEvent {
    }
}
