package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class TypeSchematicPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("Доп. смещение по Y после привязки к поверхности (+ выше, − ниже)."))
    public int verticalOffset = 0;

    @Comment(@CommentValue("Макс. перепад высот footprint (блоки)."))
    public int maxSurfaceDelta = 32;

    @Comment(@CommentValue("Макс. перепад в safety-margin вокруг footprint (относительно пола схемы)."))
    public int maxSafetyMarginDelta = 4;

    @Comment(@CommentValue("Подгонка рельефа перед paste: сколько блоков вверх/вниз по каждой колонке footprint."))
    public int terrainAdaptBlocks = 32;

    @Comment(@CommentValue("Кольцо подгонки рельефа снаружи контура solid-пола (блоки), 0 = выкл."))
    public int terrainApproachRing = 0;

    @Comment(@CommentValue("Мин. воздуха над верхом схемы."))
    public int minAirAbove = 6;

    @Comment(@CommentValue("Кольцо проверки вокруг footprint (обрывы, вода)."))
    public int safetyMargin = 4;

    @Comment(@CommentValue("Шаг сетки probe для крупных схем (0 = авто, ~56 точек на footprint)."))
    public int placementProbeStep = 0;

    @Comment(@CommentValue("Отклонять воду/лаву в зоне."))
    public boolean rejectLiquids = true;

    @Comment(@CommentValue("Горизонтальный радиус от probe: вода/лава в колонке = отказ (берега рек)."))
    public int rejectWaterWithinHorizontalBlocks = 5;

    @Comment(@CommentValue("Сколько блоков вниз от поверхности смотреть на воду/лаву у края."))
    public int rejectWaterDepthBlocks = 8;

    @Comment(@CommentValue("Мин. блоков от края solid-пола до воды/лавы (0 = legacy rejectWaterWithinHorizontalBlocks)."))
    public int minWaterClearanceFromEdge = 0;

    @Comment(@CommentValue("Мин. блоков от края solid-пола до зоны обрыва/склона."))
    public int minCliffClearanceFromEdge = 0;

    @Comment(@CommentValue("Макс. перепад Y в зоне обрыва относительно ближайшей колонки пола."))
    public int maxCliffDropFromEdge = 5;

    @Comment(@CommentValue("Твёрдый блок под поверхностью."))
    public boolean requireSolidBelow = false;

    @Comment(@CommentValue("Материалы подгонки рельефа под footprint."))
    public TypeSchematicTerrainMaterialsSettings terrainMaterials = new TypeSchematicTerrainMaterialsSettings();
}
