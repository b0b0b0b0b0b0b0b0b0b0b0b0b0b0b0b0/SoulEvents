package bm.b0b0b0.soulevents.mobwaves.util;

import bm.b0b0b0.soulevents.mobwaves.config.settings.MobPotionEffectSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MobPotionEffectSupport {

    private static final List<PotionEffectType> GUI_TYPES = List.of(
            PotionEffectType.SPEED,
            PotionEffectType.SLOWNESS,
            PotionEffectType.STRENGTH,
            PotionEffectType.WEAKNESS,
            PotionEffectType.RESISTANCE,
            PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.REGENERATION,
            PotionEffectType.INVISIBILITY,
            PotionEffectType.GLOWING,
            PotionEffectType.POISON,
            PotionEffectType.WITHER,
            PotionEffectType.HASTE,
            PotionEffectType.MINING_FATIGUE,
            PotionEffectType.JUMP_BOOST,
            PotionEffectType.NAUSEA,
            PotionEffectType.BLINDNESS,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.ABSORPTION
    );

    private MobPotionEffectSupport() {
    }

    public static List<PotionEffectType> guiTypes() {
        List<PotionEffectType> types = new ArrayList<>();
        for (PotionEffectType type : GUI_TYPES) {
            if (type != null) {
                types.add(type);
            }
        }
        return types;
    }

    public static PotionEffectType resolve(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        PotionEffectType type = PotionEffectType.getByName(normalized);
        if (type != null) {
            return type;
        }
        return Registry.EFFECT.get(NamespacedKey.minecraft(normalized.toLowerCase(Locale.ROOT)));
    }

    public static PotionEffectType nextGuiType(String current, List<MobPotionEffectSettings> existing) {
        List<PotionEffectType> types = guiTypes();
        if (types.isEmpty()) {
            return PotionEffectType.SPEED;
        }
        int start = 0;
        PotionEffectType resolved = resolve(current);
        if (resolved != null) {
            int index = types.indexOf(resolved);
            if (index >= 0) {
                start = index + 1;
            }
        }
        for (int offset = 0; offset < types.size(); offset++) {
            PotionEffectType candidate = types.get((start + offset) % types.size());
            if (!containsType(existing, candidate)) {
                return candidate;
            }
        }
        return types.get(start % types.size());
    }

    public static List<MobPotionEffectSettings> merge(
            List<MobPotionEffectSettings> base,
            List<MobPotionEffectSettings> extra
    ) {
        List<MobPotionEffectSettings> merged = new ArrayList<>();
        if (base != null) {
            merged.addAll(base);
        }
        if (extra != null) {
            merged.addAll(extra);
        }
        return merged;
    }

    public static void apply(LivingEntity living, List<MobPotionEffectSettings> effects) {
        if (living == null || effects == null || effects.isEmpty()) {
            return;
        }
        for (MobPotionEffectSettings entry : effects) {
            PotionEffectType type = resolve(entry.type);
            if (type == null) {
                continue;
            }
            int duration = entry.durationTicks < 0
                    ? PotionEffect.INFINITE_DURATION
                    : Math.max(1, entry.durationTicks);
            int amplifier = Math.max(0, entry.amplifier);
            living.addPotionEffect(new PotionEffect(
                    type,
                    duration,
                    amplifier,
                    entry.ambient,
                    entry.particles,
                    true
            ));
        }
    }

    public static Material iconMaterial(PotionEffectType type) {
        if (type == null) {
            return Material.POTION;
        }
        if (type.equals(PotionEffectType.SPEED) || type.equals(PotionEffectType.SLOWNESS)) {
            return Material.SUGAR;
        }
        if (type.equals(PotionEffectType.STRENGTH) || type.equals(PotionEffectType.WEAKNESS)) {
            return Material.BLAZE_POWDER;
        }
        if (type.equals(PotionEffectType.RESISTANCE)) {
            return Material.IRON_CHESTPLATE;
        }
        if (type.equals(PotionEffectType.FIRE_RESISTANCE)) {
            return Material.MAGMA_CREAM;
        }
        if (type.equals(PotionEffectType.REGENERATION)) {
            return Material.GHAST_TEAR;
        }
        if (type.equals(PotionEffectType.POISON) || type.equals(PotionEffectType.WITHER)) {
            return Material.SPIDER_EYE;
        }
        if (type.equals(PotionEffectType.INVISIBILITY)) {
            return Material.FERMENTED_SPIDER_EYE;
        }
        if (type.equals(PotionEffectType.GLOWING)) {
            return Material.GLOWSTONE_DUST;
        }
        return Material.POTION;
    }

    public static Component displayName(PotionEffectType type, int amplifier) {
        if (type == null) {
            return Component.text("?");
        }
        Component name = Component.translatable(type.translationKey());
        if (amplifier > 0) {
            return name.append(Component.text(" " + romanLevel(amplifier + 1)));
        }
        return name;
    }

    public static String formatSummary(MobPotionEffectSettings entry) {
        PotionEffectType type = resolve(entry.type);
        String label = type == null ? entry.type : type.getKey().getKey().toUpperCase(Locale.ROOT);
        return label + " " + romanLevel(entry.amplifier + 1);
    }

    private static boolean containsType(List<MobPotionEffectSettings> existing, PotionEffectType type) {
        if (type == null) {
            return false;
        }
        String name = type.getKey().getKey().toUpperCase(Locale.ROOT);
        for (MobPotionEffectSettings entry : existing) {
            PotionEffectType resolved = resolve(entry.type);
            if (resolved != null && resolved.equals(type)) {
                return true;
            }
            if (entry.type != null && entry.type.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static String romanLevel(int level) {
        return switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> Integer.toString(level);
        };
    }
}
