package bm.b0b0b0.soulevents.mobwaves.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MobWaveEntitySupport {

    private static final EnumSet<EntityType> BANNED = EnumSet.of(
            EntityType.EVOKER,
            EntityType.ENDER_DRAGON
    );

    private MobWaveEntitySupport() {
    }

    public static boolean isAllowed(EntityType type) {
        return type != null
                && type.isAlive()
                && type.isSpawnable()
                && !BANNED.contains(type)
                && type != EntityType.PLAYER
                && type != EntityType.ARMOR_STAND;
    }

    public static List<EntityType> allowedTypes() {
        List<EntityType> types = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (isAllowed(type)) {
                types.add(type);
            }
        }
        types.sort(Comparator.comparing(Enum::name));
        return types;
    }

    public static Optional<EntityType> fromSpawnEgg(Material material) {
        if (material == null || !material.name().endsWith("_SPAWN_EGG")) {
            return Optional.empty();
        }
        String entityName = material.name().substring(0, material.name().length() - "_SPAWN_EGG".length());
        EntityType type = resolveEntityType(entityName);
        if (type == null || !isAllowed(type)) {
            return Optional.empty();
        }
        return Optional.of(type);
    }

    public static Material spawnEgg(EntityType type) {
        if (type == null) {
            return Material.BARRIER;
        }
        Material material = Material.matchMaterial(type.name() + "_SPAWN_EGG");
        return material == null ? Material.BARRIER : material;
    }

    public static EntityType nextAllowedType(String current) {
        List<EntityType> types = allowedTypes();
        if (types.isEmpty()) {
            return EntityType.ZOMBIE;
        }
        if (current == null || current.isBlank()) {
            return types.getFirst();
        }
        EntityType resolved = resolveEntityType(current);
        int index = resolved == null ? -1 : types.indexOf(resolved);
        if (index < 0) {
            return types.getFirst();
        }
        return types.get((index + 1) % types.size());
    }

    public static EntityType resolveEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        try {
            EntityType type = EntityType.valueOf(normalized);
            return isAllowed(type) ? type : null;
        } catch (IllegalArgumentException ignored) {
            EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(normalized.toLowerCase(Locale.ROOT)));
            return isAllowed(type) ? type : null;
        }
    }
}
