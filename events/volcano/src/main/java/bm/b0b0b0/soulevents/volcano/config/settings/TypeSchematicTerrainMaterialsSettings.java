package bm.b0b0b0.soulevents.volcano.config.settings;

import bm.b0b0b0.soulevents.api.schematic.SchematicTerrainPreset;
import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public final class TypeSchematicTerrainMaterialsSettings extends YamlSerializable {

    @Comment(@CommentValue("Профиль материалов подгонки рельефа. CUSTOM = только extra/exclude."))
    public SchematicTerrainPreset preset = SchematicTerrainPreset.OVERWORLD;

    @Comment(@CommentValue("Доп. блоки верхнего слоя (Material)."))
    public List<Material> extraNaturalTop = new ArrayList<>();

    @Comment(@CommentValue("Доп. блоки, которые можно срезать."))
    public List<Material> extraRemovable = new ArrayList<>();

    @Comment(@CommentValue("Исключить из верхнего слоя (Material)."))
    public List<Material> excludeNaturalTop = new ArrayList<>();

    @Comment(@CommentValue("Исключить из срезаемых (Material)."))
    public List<Material> excludeRemovable = new ArrayList<>();
}
