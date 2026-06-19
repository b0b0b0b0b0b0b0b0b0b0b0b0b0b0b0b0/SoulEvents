package bm.b0b0b0.soulevents.volcano.config.settings;

import bm.b0b0b0.soulevents.api.schematic.SchematicBlendPreset;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class TypeSchematicBlendMaterialsSettings extends YamlSerializable {

    @Comment(@CommentValue("Профиль replaceable для blend. CUSTOM = только extra/exclude."))
    public SchematicBlendPreset preset = SchematicBlendPreset.OVERWORLD;

    @Comment(@CommentValue("Доп. replaceable блоки (Material)."))
    public List<Material> extraReplaceable = new ArrayList<>();

    @Comment(@CommentValue("Исключить из replaceable (Material)."))
    public List<Material> excludeReplaceable = new ArrayList<>();
}
