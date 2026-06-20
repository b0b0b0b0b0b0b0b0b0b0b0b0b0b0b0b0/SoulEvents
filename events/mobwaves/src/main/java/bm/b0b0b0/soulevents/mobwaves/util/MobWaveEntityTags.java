package bm.b0b0b0.soulevents.mobwaves.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.UUID;

public final class MobWaveEntityTags {

    private static NamespacedKey sessionKey(Plugin plugin) {
        return new NamespacedKey(plugin, "mobwave-session");
    }

    private static NamespacedKey displayKey(Plugin plugin) {
        return new NamespacedKey(plugin, "mobwave-display");
    }

    private MobWaveEntityTags() {
    }

    private static NamespacedKey bossKey(Plugin plugin) {
        return new NamespacedKey(plugin, "mobwave-boss");
    }

    public static void tagMob(Plugin plugin, Entity entity, UUID sessionId, boolean superBoss) {
        tagMob(plugin, entity, sessionId);
        if (superBoss) {
            entity.getPersistentDataContainer().set(bossKey(plugin), PersistentDataType.BYTE, (byte) 1);
        }
    }

    public static void tagMob(Plugin plugin, Entity entity, UUID sessionId) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        container.set(sessionKey(plugin), PersistentDataType.STRING, sessionId.toString());
    }

    public static void tagDisplay(Plugin plugin, Entity display, UUID mobId) {
        PersistentDataContainer container = display.getPersistentDataContainer();
        container.set(displayKey(plugin), PersistentDataType.STRING, mobId.toString());
    }

    public static Optional<UUID> sessionId(Plugin plugin, Entity entity) {
        String raw = entity.getPersistentDataContainer().get(sessionKey(plugin), PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean isSuperBoss(Plugin plugin, Entity entity) {
        Byte value = entity.getPersistentDataContainer().get(bossKey(plugin), PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public static boolean isMobWaveEntity(Plugin plugin, Entity entity) {
        return sessionId(plugin, entity).isPresent();
    }
}
