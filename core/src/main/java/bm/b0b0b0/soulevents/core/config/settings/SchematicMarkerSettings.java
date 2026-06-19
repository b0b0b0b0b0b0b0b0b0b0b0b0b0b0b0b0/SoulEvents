package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicMarkerSettings extends YamlSerializable {

    @Comment(@CommentValue("Блок-маркер точки сундука в .schem (обычно BEDROCK)."))
    public String block = "BEDROCK";

    @Comment(@CommentValue("Искать маркер при загрузке .schem."))
    public boolean autoDetect = true;

    @Comment(@CommentValue("Удалить маркер при вставке (заменить на AIR)."))
    public boolean replaceWithAir = true;

    @Comment(@CommentValue("Ручной offset сундука от origin схемы, если autoDetect: false."))
    public int chestOffsetX = 0;

    public int chestOffsetY = 0;

    public int chestOffsetZ = 0;
}
