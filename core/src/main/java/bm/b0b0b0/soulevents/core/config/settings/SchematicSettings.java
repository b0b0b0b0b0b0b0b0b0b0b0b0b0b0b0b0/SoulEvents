package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Runtime-настройки схемы. В schematics/<id>.yml на диске — только marker (см. SchematicFileSettings)."))
public final class SchematicSettings extends YamlSerializable {

    public SchematicPlacementSettings placement = new SchematicPlacementSettings();

    public SchematicMarkerSettings marker = new SchematicMarkerSettings();

    public SchematicPasteSettings paste = new SchematicPasteSettings();

    public SchematicBlendSettings blend = new SchematicBlendSettings();
}
