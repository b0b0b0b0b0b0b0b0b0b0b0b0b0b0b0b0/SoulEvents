package bm.b0b0b0.soulevents.api.module;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface EventModuleRegistry {

    void register(EventModule module);

    void setRegisterListener(Consumer<EventModule> listener);

    void unregister(String moduleId);

    Optional<EventModule> module(String moduleId);

    Collection<EventModule> modules();

    Optional<ActiveEvent> activeEvent(UUID sessionId);

    Collection<ActiveEvent> activeEvents();

    Collection<ActiveEvent> activeEvents(String moduleId);
}
