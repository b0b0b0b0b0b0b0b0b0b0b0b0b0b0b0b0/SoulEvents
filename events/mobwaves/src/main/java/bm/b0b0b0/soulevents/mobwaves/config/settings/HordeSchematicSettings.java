package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeSchematicSettings extends YamlSerializable {

    @Comment(@CommentValue("Схема из plugins/SoulEvents/schematics/<id>.schem вместо встроенного нексуса."))
    public boolean enabled = false;

    @Comment(@CommentValue("Имя .schem без расширения. Bedrock-маркер в схеме = точка выхода орды."))
    public String id = "horde_rift";

    @Comment(@CommentValue("Lang-ключ названия структуры в broadcast."))
    public String structureNameKey = "mobwaves.structure.rift-nexus";

    public HordeSchematicPlacementSettings placement = new HordeSchematicPlacementSettings();

    public HordeSchematicPasteSettings paste = new HordeSchematicPasteSettings();

    public HordeSchematicBlendSettings blend = new HordeSchematicBlendSettings();

    @Comment(@CommentValue("Маркеры bedrock в .schem — верхняя точка спавна волн."))
    public HordeSchematicMarkerSettings marker = new HordeSchematicMarkerSettings();
}
