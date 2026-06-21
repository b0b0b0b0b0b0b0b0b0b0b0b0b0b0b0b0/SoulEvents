package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class MobTypeOverrideSettings extends YamlSerializable {

    @Comment(@CommentValue("Переопределение здоровья. 0 = дефолт типа."))
    public double maxHealth = 0.0;

    @Comment(@CommentValue("Множитель скорости. 1 = дефолт, 1.2 = +20%."))
    public double speedMultiplier = 1.0;

    @Comment(@CommentValue("Множитель урона атаки."))
    public double damageMultiplier = 1.0;

    @Comment(@CommentValue("Эффекты зелий (SPEED, STRENGTH, …). duration-ticks: -1 = бесконечно."))
    public List<MobPotionEffectSettings> effects = new ArrayList<>();
}
