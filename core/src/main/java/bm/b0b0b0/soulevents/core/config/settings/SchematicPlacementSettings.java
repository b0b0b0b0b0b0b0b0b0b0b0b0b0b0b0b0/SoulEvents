package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("Доп. смещение по Y после привязки к поверхности (+ выше, − ниже)."))
    public int verticalOffset = 0;

    @Comment(@CommentValue("Макс. перепад высот footprint (блоки). Для крупных схем обычно хватает 3."))
    public int maxSurfaceDelta = 3;

    @Comment(@CommentValue("Подгонка рельефа перед paste: сколько блоков вверх/вниз по каждой колонке footprint."))
    public int terrainAdaptBlocks = 3;

    @Comment(@CommentValue("Кольцо ступенек наружу от контура solid-пола (воздух схемы игнорируется), 0 = выкл."))
    public int terrainApproachRing = 0;

    @Comment(@CommentValue("Мин. воздуха над верхом схемы."))
    public int minAirAbove = 6;

    @Comment(@CommentValue("Кольцо проверки вокруг footprint (обрывы, вода)."))
    public int safetyMargin = 4;

    @Comment(@CommentValue("Шаг сетки probe для крупных схем (0 = авто, ~56 точек на footprint)."))
    public int placementProbeStep = 0;

    @Comment(@CommentValue("Отклонять воду/лаву в зоне."))
    public boolean rejectLiquids = true;

    @Comment(@CommentValue("Твёрдый блок под поверхностью."))
    public boolean requireSolidBelow = true;

    @Comment(@CommentValue("Материалы подгонки рельефа под footprint."))
    public SchematicTerrainMaterialsSettings terrainMaterials = new SchematicTerrainMaterialsSettings();
}
