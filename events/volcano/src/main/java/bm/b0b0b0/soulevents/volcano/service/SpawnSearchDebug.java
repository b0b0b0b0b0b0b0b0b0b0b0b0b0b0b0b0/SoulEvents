package bm.b0b0b0.soulevents.volcano.service;

import org.bukkit.plugin.Plugin;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class SpawnSearchDebug {

    private final Plugin plugin;
    private final boolean enabled;
    private final String typeId;
    private final Map<String, Integer> rejectionCounts = new LinkedHashMap<>();
    private int attempts;
    private String lastFailureReason;

    SpawnSearchDebug(Plugin plugin, boolean enabled, String typeId) {
        this.plugin = plugin;
        this.enabled = enabled;
        this.typeId = typeId;
    }

    boolean enabled() {
        return enabled;
    }

    void start(String worldName, MapSpawnBoundary.Area area, RandomLocationFinder.SearchProfile profile) {
        if (!enabled) {
            return;
        }
        log("search start world=" + worldName
                + " schematic=" + profile.schematic()
                + " schematicId=" + profile.schematicId()
                + " maxAttempts=" + profile.maxAttempts()
                + " center=" + area.centerX() + "," + area.centerZ()
                + " radius=" + area.minRadius() + "-" + area.maxRadius()
                + (area.hasBoundary() ? " mapBoundary=" + area.boundaryRadius() : "")
                + " flatSurface=" + profile.requireFlatSurface()
                + " skipWaterBiomes=" + profile.skipWaterBiomes()
                + " surfaceDeltaLimit=" + profile.surfaceDeltaLimit()
                + " timeoutSec=" + profile.searchTimeoutSeconds()
                + " parallel=" + profile.parallelAttempts()
                + " loadedLimit=" + profile.loadedChunkCandidateLimit());
    }

    void reject(int attemptIndex, int total, int x, int z, String reason) {
        attempts++;
        rejectionCounts.merge(categorizeReason(reason), 1, Integer::sum);
        if (!enabled) {
            return;
        }
        log("attempt " + (attemptIndex + 1) + "/" + total + " x=" + x + " z=" + z + " -> " + reason);
    }

    void success(int attemptIndex, int total, int x, int y, int z) {
        if (!enabled) {
            return;
        }
        log("attempt " + (attemptIndex + 1) + "/" + total + " x=" + x + " y=" + y + " z=" + z + " -> OK");
    }

    void noCandidates(String reason) {
        lastFailureReason = reason;
        rejectionCounts.merge("no-candidates", 1, Integer::sum);
        if (!enabled) {
            return;
        }
        log("FAILED: " + reason);
    }

    void finishFailed() {
        if (!enabled) {
            return;
        }
        log("FAILED after " + attempts + " attempts. Reasons: " + formatRejectionSummary());
    }

    void finishTimedOut(int timeoutSeconds) {
        if (!enabled) {
            return;
        }
        log("TIMEOUT after " + timeoutSeconds + "s (" + attempts + " attempts). Reasons: " + formatRejectionSummary());
    }

    void finishFailedWorld(String worldName, String reason) {
        lastFailureReason = "world=" + worldName + " " + reason;
        if (!enabled) {
            return;
        }
        log("FAILED " + lastFailureReason);
    }

    String failureMessage() {
        if (lastFailureReason != null && !lastFailureReason.isEmpty()) {
            return lastFailureReason;
        }
        String summary = formatRejectionSummary();
        if (!"none".equals(summary)) {
            return summary;
        }
        return attempts > 0 ? "no valid location after " + attempts + " attempts" : "no valid location";
    }

    private String formatRejectionSummary() {
        if (rejectionCounts.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : rejectionCounts.entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return builder.toString();
    }

    String rejectionSummary() {
        return formatRejectionSummary();
    }

    private void log(String message) {
        plugin.getLogger().info("[Volcano-Spawn][" + typeId + "] " + message);
    }

    static String gateReason(String denial, String detail) {
        if (detail == null || detail.isEmpty()) {
            return "gate-" + denial.toLowerCase(Locale.ROOT);
        }
        return "gate-" + denial.toLowerCase(Locale.ROOT) + "(" + detail + ")";
    }

    static String categorizeReason(String reason) {
        if (reason == null || reason.isEmpty()) {
            return "unknown";
        }
        if (reason.startsWith("schematic-terrain-too-rough")) {
            return "schematic-terrain-too-rough";
        }
        if (reason.startsWith("schematic-surface-invalid")) {
            return "schematic-surface-invalid";
        }
        if (reason.startsWith("schematic-clearance-blocked")) {
            return "schematic-clearance-blocked";
        }
        if (reason.startsWith("schematic-")) {
            return reason.contains(" ") ? reason.substring(0, reason.indexOf(' ')) : reason;
        }
        if (reason.startsWith("chunk-unloaded") || reason.startsWith("chunk-load-failed")) {
            return "chunk-load-failed";
        }
        if (reason.startsWith("water-surface") || reason.startsWith("water-biome")) {
            return "water-surface";
        }
        if (reason.startsWith("flat-")) {
            return reason.contains(" ") ? reason.substring(0, reason.indexOf(' ')) : reason;
        }
        if (reason.startsWith("gate-")) {
            return reason.contains("(") ? reason.substring(0, reason.indexOf('(')) : reason;
        }
        return reason;
    }
}

