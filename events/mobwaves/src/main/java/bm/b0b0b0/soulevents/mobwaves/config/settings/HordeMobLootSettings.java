package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeMobLootSettings extends YamlSerializable {

    @Comment(@CommentValue("Сколько раз роллить лут с одного убитого моба (из пула/таблицы)."))
    public int rollsPerKillMin = 1;

    public int rollsPerKillMax = 1;

    @Comment(@CommentValue("Pickup delay на Item entity (тики)."))
    public int pickupDelayTicks = 15;

    @Comment(@CommentValue("Небольшой разброс при выпадении."))
    public double dropVelocityHorizontal = 0.12;

    public double dropVelocityVertical = 0.25;
}
