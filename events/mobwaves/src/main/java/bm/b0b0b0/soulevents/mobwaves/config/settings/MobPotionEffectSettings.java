package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class MobPotionEffectSettings extends YamlSerializable {

    @Comment(@CommentValue("Тип эффекта: SPEED, STRENGTH, RESISTANCE, FIRE_RESISTANCE, REGENERATION, …"))
    public String type = "SPEED";

    @Comment(@CommentValue("Уровень минус 1. 0 = I, 1 = II, 2 = III."))
    public int amplifier = 0;

    @Comment(@CommentValue("Длительность в тиках. -1 = пока моб жив."))
    public int durationTicks = -1;

    public boolean ambient = false;

    public boolean particles = true;
}
