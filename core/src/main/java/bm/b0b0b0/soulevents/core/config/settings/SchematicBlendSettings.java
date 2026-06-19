package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicBlendSettings extends YamlSerializable {

    @Comment(@CommentValue("Сглаживание краёв схемы в ландшафт."))
    public boolean enabled = true;

    @Comment(@CommentValue("Радиус blend."))
    public int radius = 4;

    @Comment(@CommentValue("Материалы, которые blend может заменить."))
    public SchematicBlendMaterialsSettings materials = new SchematicBlendMaterialsSettings();
}
