package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class EffectProfileSettings extends YamlSerializable {

    @Comment(@CommentValue("Радиус действия профиля (блоки от якоря)."))
    public int radius = 32;

    @Comment(@CommentValue("Интервал повторного наложения эффектов (тики)."))
    public int tickInterval = 40;

    @Comment(@CommentValue("Список эффектов профиля."))
    public List<PotionEffectEntrySettings> effects = defaultEffects();

    private static List<PotionEffectEntrySettings> defaultEffects() {
        List<PotionEffectEntrySettings> effects = new ArrayList<>();
        PotionEffectEntrySettings slowness = new PotionEffectEntrySettings();
        slowness.type = "SLOWNESS";
        slowness.amplifier = 1;
        slowness.durationTicks = 120;
        effects.add(slowness);
        PotionEffectEntrySettings weakness = new PotionEffectEntrySettings();
        weakness.type = "WEAKNESS";
        weakness.durationTicks = 120;
        effects.add(weakness);
        PotionEffectEntrySettings glow = new PotionEffectEntrySettings();
        glow.type = "GLOWING";
        glow.durationTicks = 120;
        effects.add(glow);
        return effects;
    }
}
