package bm.b0b0b0.soulevents.core.protection;

import bm.b0b0b0.soulevents.core.config.settings.EffectProfileSettings;
import bm.b0b0b0.soulevents.core.config.settings.PotionEffectEntrySettings;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class EffectProfileSupport {

    private EffectProfileSupport() {
    }

    static List<PotionEffect> toPotionEffects(Map<String, EffectProfileSettings> profiles, String profileId) {
        EffectProfileSettings profile = profiles.get(profileId);
        if (profile == null) {
            profile = profiles.get("default");
        }
        if (profile == null || profile.effects.isEmpty()) {
            return List.of();
        }
        List<PotionEffect> resolved = new ArrayList<>();
        for (PotionEffectEntrySettings entry : profile.effects) {
            PotionEffectType type = resolveType(entry.type);
            if (type == null) {
                continue;
            }
            resolved.add(new PotionEffect(
                    type,
                    Math.max(1, entry.durationTicks),
                    Math.max(0, entry.amplifier),
                    entry.ambient,
                    entry.particles,
                    entry.icon
            ));
        }
        return List.copyOf(resolved);
    }

    static EffectProfileSettings resolveProfile(Map<String, EffectProfileSettings> profiles, String profileId) {
        EffectProfileSettings profile = profiles.get(profileId);
        if (profile != null) {
            return profile;
        }
        EffectProfileSettings fallback = profiles.get("default");
        return fallback == null ? new EffectProfileSettings() : fallback;
    }

    private static PotionEffectType resolveType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.contains(":")) {
            NamespacedKey namespacedKey = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
            return namespacedKey == null ? null : Registry.POTION_EFFECT_TYPE.get(namespacedKey);
        }
        PotionEffectType direct = Registry.POTION_EFFECT_TYPE.get(
                NamespacedKey.minecraft(trimmed.toLowerCase(Locale.ROOT))
        );
        if (direct != null) {
            return direct;
        }
        return legacyAlias(trimmed.toUpperCase(Locale.ROOT));
    }

    private static PotionEffectType legacyAlias(String normalized) {
        return switch (normalized) {
            case "SLOW" -> PotionEffectType.SLOWNESS;
            case "FAST" -> PotionEffectType.SPEED;
            case "HARM", "INSTANT_DAMAGE" -> PotionEffectType.INSTANT_DAMAGE;
            case "HEAL", "INSTANT_HEAL" -> PotionEffectType.INSTANT_HEALTH;
            case "JUMP" -> PotionEffectType.JUMP_BOOST;
            default -> null;
        };
    }
}
