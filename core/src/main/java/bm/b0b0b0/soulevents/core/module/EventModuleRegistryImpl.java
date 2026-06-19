package bm.b0b0b0.soulevents.core.module;

import bm.b0b0b0.soulevents.api.module.ActiveEvent;
import bm.b0b0b0.soulevents.api.module.EventModule;
import bm.b0b0b0.soulevents.api.module.EventModuleRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class EventModuleRegistryImpl implements EventModuleRegistry {

    private final Map<String, EventModule> modules = new LinkedHashMap<>();
    private final EventSessionControllerImpl sessions;
    private Consumer<EventModule> registerListener;

    public EventModuleRegistryImpl(EventSessionControllerImpl sessions) {
        this.sessions = sessions;
    }

    @Override
    public void setRegisterListener(Consumer<EventModule> listener) {
        this.registerListener = listener;
    }

    @Override
    public void register(EventModule module) {
        modules.put(module.id(), module);
        if (registerListener != null) {
            registerListener.accept(module);
        }
    }

    @Override
    public void unregister(String moduleId) {
        modules.remove(moduleId);
    }

    @Override
    public Optional<EventModule> module(String moduleId) {
        return Optional.ofNullable(modules.get(moduleId));
    }

    @Override
    public Collection<EventModule> modules() {
        return List.copyOf(modules.values());
    }

    @Override
    public Optional<ActiveEvent> activeEvent(UUID sessionId) {
        return sessions.get(sessionId);
    }

    @Override
    public Collection<ActiveEvent> activeEvents() {
        return sessions.all();
    }

    @Override
    public Collection<ActiveEvent> activeEvents(String moduleId) {
        List<ActiveEvent> result = new ArrayList<>();
        for (ActiveEvent event : sessions.all()) {
            if (event.moduleId().equals(moduleId)) {
                result.add(event);
            }
        }
        return List.copyOf(result);
    }
}
