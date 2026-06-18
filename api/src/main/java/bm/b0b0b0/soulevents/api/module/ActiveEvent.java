package bm.b0b0b0.soulevents.api.module;

import org.bukkit.Location;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ActiveEvent {

    UUID sessionId();

    String moduleId();

    String typeId();

    EventPhase phase();

    Location anchor();

    Instant startedAt();

    Optional<Instant> lootableAt();
}
