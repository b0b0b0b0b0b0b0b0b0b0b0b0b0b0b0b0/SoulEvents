package bm.b0b0b0.soulevents.mobwaves.util;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;

public final class MobWaveEntityNames {

    private MobWaveEntityNames() {
    }

    public static Component displayName(EntityType type) {
        if (type == null) {
            return Component.text("?");
        }
        return Component.translatable(type.translationKey());
    }
}
