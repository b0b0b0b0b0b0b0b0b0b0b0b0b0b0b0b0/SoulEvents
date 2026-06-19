package bm.b0b0b0.soulevents.volcano.config;

import java.util.Locale;
import java.util.regex.Pattern;

public final class ConfigIds {

    private static final Pattern SAFE_ID = Pattern.compile("^[a-z0-9][a-z0-9._-]{0,63}$");

    private ConfigIds() {
    }

    public static boolean isValid(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("..") || normalized.indexOf('/') >= 0 || normalized.indexOf('\\') >= 0) {
            return false;
        }
        return SAFE_ID.matcher(normalized).matches();
    }

    public static String requireValid(String id) {
        if (!isValid(id)) {
            throw new IllegalArgumentException("Invalid config id: " + id);
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }
}

