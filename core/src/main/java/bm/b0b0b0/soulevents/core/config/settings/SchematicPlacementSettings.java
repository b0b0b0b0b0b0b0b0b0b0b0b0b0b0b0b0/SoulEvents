package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class SchematicPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("Доп. смещение по Y после привязки к поверхности (+ выше, − ниже)."))
    public int verticalOffset = 0;

    @Comment(@CommentValue("Макс. перепад высот footprint (блоки). Для крупных схем обычно хватает 3."))
    public int maxSurfaceDelta = 3;

    @Comment(@CommentValue("Макс. перепад в safety-margin вокруг footprint (относительно пола схемы)."))
    public int maxSafetyMarginDelta = 4;

    @Comment(@CommentValue("Подгонка рельефа перед paste: сколько блоков вверх/вниз по каждой колонке footprint."))
    public int terrainAdaptBlocks = 3;

    @Comment(@CommentValue("Кольцо ступенек наружу от контура solid-пола: подсыпка вниз и срезка подъёма, 0 = выкл."))
    public int terrainApproachRing = 0;

    @Comment(@CommentValue("Полоса подгонки почвы только ПЕРЕД схемой (блоки наружу), 0 = выкл. Не кольцо вокруг."))
    public int terrainApproachFrontDepth = 0;

    @Comment(@CommentValue("Сторона фронта: AUTO (к маркеру/жерлу), NORTH, SOUTH, EAST, WEST — в осях схемы."))
    public String approachFrontFacing = "AUTO";

    @Comment(@CommentValue("Снаружи периметра: только срез лишнего +1 блок (без dirt/grass подсыпки)."))
    public boolean terrainApproachTrimOnly = false;

    @Comment(@CommentValue("Доля колонок среза при trim-only (0.5–0.85 ≈ рваный край как в генерации)."))
    public float terrainApproachRaggedDensity = 0.62f;

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

    @Comment(@CommentValue("Мин. блоков от края solid-пола до воды/лавы (0 = старый rejectWaterWithinHorizontalBlocks)."))
    public int minWaterClearanceFromEdge = 6;

    @Comment(@CommentValue("Мин. блоков от края solid-пола до зоны обрыва/склона."))
    public int minCliffClearanceFromEdge = 4;

    @Comment(@CommentValue("Макс. перепад Y в зоне обрыва относительно ближайшей колонки пола."))
    public int maxCliffDropFromEdge = 8;

    @Comment(@CommentValue("Сколько блоков вниз от поверхности смотреть на воду/лаву у края."))
    public int rejectWaterDepthBlocks = 8;

    @Comment(@CommentValue("Твёрдый блок под поверхностью."))
    public boolean requireSolidBelow = true;

    @Comment(@CommentValue("Материалы подгонки рельефа под footprint."))
    public SchematicTerrainMaterialsSettings terrainMaterials = new SchematicTerrainMaterialsSettings();
}
