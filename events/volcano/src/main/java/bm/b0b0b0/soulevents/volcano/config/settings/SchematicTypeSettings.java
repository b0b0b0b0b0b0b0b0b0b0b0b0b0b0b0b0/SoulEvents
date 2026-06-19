package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicTypeSettings extends YamlSerializable {

    public static final String BUNDLED_SCHEMATIC_ID = "test_volcano";

    @Comment(@CommentValue("Вставлять схему из plugins/SoulEvents/schematics/ при спавне."))
    public boolean enabled = true;

    @Comment(@CommentValue("Имя схемы без .schem (файл <id>.schem в schematics/)."))
    public String id = BUNDLED_SCHEMATIC_ID;

    @Comment(@CommentValue("Размещение на карте и подгонка рельефа — настройки для этого типа аирдропа."))
    public TypeSchematicPlacementSettings placement = new TypeSchematicPlacementSettings();

    @Comment(@CommentValue("Параметры FAWE paste."))
    public TypeSchematicPasteSettings paste = new TypeSchematicPasteSettings();

    @Comment(@CommentValue("Сглаживание краёв после paste."))
    public TypeSchematicBlendSettings blend = new TypeSchematicBlendSettings();

    @Comment(@CommentValue("Bedrock-маркеры в .schem: сколько точек лута активировать за спавн."))
    public TypeSchematicMarkerSettings marker = new TypeSchematicMarkerSettings();
}
