package bm.b0b0b0.soulevents.api.module;

import org.bukkit.Location;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EventSessionController {

    ActiveEvent start(String moduleId, String typeId, Location anchor);

    void setPhase(UUID sessionId, EventPhase phase);

    void setLootableAt(UUID sessionId, Instant lootableAt);

    void end(UUID sessionId);

    Optional<ActiveEvent> get(UUID sessionId);
}
