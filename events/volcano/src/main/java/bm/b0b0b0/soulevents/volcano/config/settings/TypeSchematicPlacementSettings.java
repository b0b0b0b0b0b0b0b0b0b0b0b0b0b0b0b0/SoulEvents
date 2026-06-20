package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class TypeSchematicPlacementSettings extends YamlSerializable {

    @Comment(@CommentValue("Доп. Y после привязки подножия к земле. 0 = нижний solid схемы на motion-blocking блоке, без +1."))
    public int verticalOffset = 0;

    @Comment(@CommentValue("Макс. перепад высот подножия по footprint; склоны отклоняются."))
    public int maxSurfaceDelta = 5;

    @Comment(@CommentValue("Нормализация подножия: подгонка всего footprint вверх/вниз (блоков)."))
    public int terrainAdaptBlocks = 6;

    @Comment(@CommentValue("0 = не трогать блоки за контуром footprint (полное кольцо вокруг)."))
    public int terrainApproachRing = 0;

    @Comment(@CommentValue("Полоса снаружи: подсыпка ям + рagged-срез +1. 0 = выкл."))
    public int terrainApproachFrontDepth = 5;

    @Comment(@CommentValue("Сторона подхода: AUTO (к маркеру жерла), NORTH, SOUTH, EAST, WEST — оси схемы."))
    public String approachFrontFacing = "AUTO";

    @Comment(@CommentValue("true = снаружи только ragged-срез лишнего +1; ямы всё равно подсыпаются."))
    public boolean terrainApproachTrimOnly = true;

    @Comment(@CommentValue("Доля колонок среза (0.62 ≈ рваный край как в генерации)."))
    public float terrainApproachRaggedDensity = 0.62f;

    @Comment(@CommentValue("Макс. перепад в safety-margin вокруг footprint (legacy)."))
    public int maxSafetyMarginDelta = 4;

    @Comment(@CommentValue("Мин. воздуха над верхом схемы."))
    public int minAirAbove = 6;

    @Comment(@CommentValue("Кольцо проверки вокруг footprint (legacy; 0 если используется edge-clearance)."))
    public int safetyMargin = 0;

    @Comment(@CommentValue("Шаг сетки probe для крупных схем (0 = авто, ~56 точек на footprint)."))
    public int placementProbeStep = 0;

    @Comment(@CommentValue("Отклонять воду/лаву в зоне."))
    public boolean rejectLiquids = true;

    @Comment(@CommentValue("Legacy: радиус от bbox (0 = minWaterClearanceFromEdge)."))
    public int rejectWaterWithinHorizontalBlocks = 0;

    @Comment(@CommentValue("Сколько блоков вниз от поверхности смотреть на воду/лаву у края."))
    public int rejectWaterDepthBlocks = 8;

    @Comment(@CommentValue("Мин. блоков от края solid-пола до воды/лавы."))
    public int minWaterClearanceFromEdge = 6;

    @Comment(@CommentValue("Мин. блоков от края solid-пола до зоны обрыва/склона."))
    public int minCliffClearanceFromEdge = 4;

    @Comment(@CommentValue("Макс. перепад Y в зоне обрыва относительно ближайшей колонки пола."))
    public int maxCliffDropFromEdge = 5;

    @Comment(@CommentValue("Твёрдый блок под поверхностью."))
    public boolean requireSolidBelow = true;

    @Comment(@CommentValue("Материалы подгонки рельефа под footprint."))
    public TypeSchematicTerrainMaterialsSettings terrainMaterials = new TypeSchematicTerrainMaterialsSettings();
}
