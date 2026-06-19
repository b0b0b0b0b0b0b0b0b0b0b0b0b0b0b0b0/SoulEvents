package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class PotionEffectEntrySettings extends YamlSerializable {

    @Comment(@CommentValue("Тип эффекта (PotionEffectType), напр. SLOWNESS."))
    public String type = "SLOWNESS";

    @Comment(@CommentValue("Длительность в тиках (20 = 1 сек)."))
    public int durationTicks = 100;

    @Comment(@CommentValue("Уровень эффекта (0 = I)."))
    public int amplifier = 0;

    @Comment(@CommentValue("Ambient-частицы."))
    public boolean ambient = true;

    @Comment(@CommentValue("Показывать частицы."))
    public boolean particles = true;

    @Comment(@CommentValue("Иконка в инвентаре."))
    public boolean icon = true;
}
