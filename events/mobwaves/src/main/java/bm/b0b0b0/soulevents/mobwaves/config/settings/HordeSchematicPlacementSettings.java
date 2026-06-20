package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Размещение схемы нексуса — мягче, чем у вулкана: монолит частично в земле."))
public final class HordeSchematicPlacementSettings extends YamlSerializable {

    public int verticalOffset = -2;

    @Comment(@CommentValue("Допустимый перепад подножия (не придираемся к рельефу)."))
    public int maxSurfaceDelta = 6;

    public int maxSafetyMarginDelta = 3;

    @Comment(@CommentValue("Подгонка колонок footprint вверх/вниз."))
    public int terrainAdaptBlocks = 4;

    public int terrainApproachRing = 2;

    public int terrainApproachFrontDepth = 0;

    public String approachFrontFacing = "AUTO";

    public boolean terrainApproachTrimOnly = true;

    public float terrainApproachRaggedDensity = 0.55f;

    public boolean terrainPerimeterRaggedTrim = true;

    public int terrainPerimeterRaggedOutwardDepth = 1;

    public int minAirAbove = 4;

    public int safetyMargin = 2;

    public int placementProbeStep = 0;

    public boolean rejectLiquids = true;

    public int rejectWaterWithinHorizontalBlocks = 4;

    public int rejectWaterDepthBlocks = 6;

    public int minWaterClearanceFromEdge = 3;

    public int minCliffClearanceFromEdge = 4;

    public int maxCliffDropFromEdge = 4;

    public int minOutwardMountainRiseSteps = 0;

    public int mountainSlopeScanDepth = 0;

    public boolean requireSolidBelow = true;

    public HordeSchematicTerrainMaterialsSettings terrainMaterials = new HordeSchematicTerrainMaterialsSettings();
}
