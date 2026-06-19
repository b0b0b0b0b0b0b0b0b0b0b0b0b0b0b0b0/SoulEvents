package bm.b0b0b0.soulevents.airdrop.config;

import java.util.regex.Pattern;

public final class ConfigIds {

    private static final Pattern SAFE_ID = Pattern.compile("[a-zA-Z0-9_-]{1,64}");

    private ConfigIds() {
    }

    public static boolean isSafe(String id) {
        return id != null && SAFE_ID.matcher(id).matches();
    }

    public static String requireSafe(String id) {
        if (!isSafe(id)) {
            throw new IllegalArgumentException("Unsafe config id (allowed: a-z A-Z 0-9 _ -, max 64): " + id);
        }
        return id;
    }
}
