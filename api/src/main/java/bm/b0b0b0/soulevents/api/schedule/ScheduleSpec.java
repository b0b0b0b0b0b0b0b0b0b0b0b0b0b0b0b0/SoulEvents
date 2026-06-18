package bm.b0b0b0.soulevents.api.schedule;

import java.time.Duration;
import java.util.List;

public record ScheduleSpec(
        boolean enabled,
        Duration interval,
        List<String> worlds,
        int maxConcurrent
) {
}
