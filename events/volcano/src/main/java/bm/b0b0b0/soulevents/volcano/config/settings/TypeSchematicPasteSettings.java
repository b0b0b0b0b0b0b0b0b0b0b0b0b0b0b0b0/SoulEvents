package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class TypeSchematicPasteSettings extends YamlSerializable {

    @Comment(@CommentValue("Не вставлять AIR-блоки из схемы."))
    public boolean ignoreAir = false;

    @Comment(@CommentValue("Лимит блоков за тик FAWE (0 = без лимита, быстрый режим)."))
    public int blocksPerTick = 0;
}
