package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicPasteSettings extends YamlSerializable {

    @Comment(@CommentValue("Не вставлять AIR-блоки из схемы."))
    public boolean ignoreAir = false;

    @Comment(@CommentValue("Лимит блоков за тик (FAWE/WE, 0 = без лимита)."))
    public int blocksPerTick = 1500;
}
