package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeSchematicBlendSettings extends YamlSerializable {

    @Comment(@CommentValue("Сглаживание краёв схемы в ландшафт после paste."))
    public boolean enabled = false;

    @Comment(@CommentValue("Радиус blend."))
    public int radius = 4;

    @Comment(@CommentValue("Материалы, которые blend может заменить."))
    public HordeSchematicBlendMaterialsSettings materials = new HordeSchematicBlendMaterialsSettings();
}
