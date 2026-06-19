package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Настройки одной схематики. Папка: plugins/SoulEvents/schematics/<id>/"))
public final class SchematicSettings extends YamlSerializable {

    @Comment(@CommentValue("Файл .schem в этой же папке."))
    public String file = "schematic.schem";

    public SchematicPlacementSettings placement = new SchematicPlacementSettings();

    public SchematicMarkerSettings marker = new SchematicMarkerSettings();

    public SchematicPasteSettings paste = new SchematicPasteSettings();

    public SchematicBlendSettings blend = new SchematicBlendSettings();
}
