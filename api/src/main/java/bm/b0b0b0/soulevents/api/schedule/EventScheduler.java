package bm.b0b0b0.soulevents.api.schedule;

import java.time.Duration;
import java.util.Optional;

public interface EventScheduler {

    void register(String moduleId, String typeId, ScheduleSpec spec, Runnable trigger);

    void unregister(String moduleId, String typeId);

    void triggerNow(String moduleId, String typeId);

    Optional<Duration> timeUntilNext(String moduleId, String typeId);
}
