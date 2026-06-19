package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("Доп. смещение по Y после привязки к поверхности (+ выше, − ниже)."))
    public int verticalOffset = 0;

    @Comment(@CommentValue("Макс. перепад высот footprint (блоки)."))
    public int maxSurfaceDelta = 2;

    @Comment(@CommentValue("Мин. воздуха над верхом схемы."))
    public int minAirAbove = 6;

    @Comment(@CommentValue("Кольцо проверки вокруг footprint (обрывы, вода)."))
    public int safetyMargin = 4;

    @Comment(@CommentValue("Отклонять воду/лаву в зоне."))
    public boolean rejectLiquids = true;

    @Comment(@CommentValue("Твёрдый блок под поверхностью."))
    public boolean requireSolidBelow = true;
}
