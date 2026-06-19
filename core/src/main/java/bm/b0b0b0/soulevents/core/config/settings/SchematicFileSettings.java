package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Маркер схемы <id>.schem — только marker. Placement/paste/blend — в types/<airdrop>.yml"))
public final class SchematicFileSettings extends YamlSerializable {

    public SchematicMarkerSettings marker = new SchematicMarkerSettings();
}
