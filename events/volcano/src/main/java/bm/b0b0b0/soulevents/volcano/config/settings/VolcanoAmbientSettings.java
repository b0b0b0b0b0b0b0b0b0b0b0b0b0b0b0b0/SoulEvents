package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class VolcanoAmbientSettings {

    @Comment(@CommentValue("Гром и молнии для игроков рядом с вулканом."))
    public boolean enabled = true;

    @Comment(@CommentValue("Радиус слышимости ambient-звуков (блоки от жерла)."))
    public double radiusBlocks = 56.0;

    @Comment(@CommentValue("Мин. пауза между громовыми звуками (тики)."))
    public int rumbleIntervalMinTicks = 100;

    @Comment(@CommentValue("Макс. пауза между громовыми звуками (тики)."))
    public int rumbleIntervalMaxTicks = 260;

    @Comment(@CommentValue("Громкость далёкого грома (только ENTITY_LIGHTNING_BOLT_THUNDER)."))
    public float rumbleVolume = 0.42f;

    @Comment(@CommentValue("Фейковые молнии без дождя (strikeLightningEffect)."))
    public boolean lightningEnabled = true;

    @Comment(@CommentValue("Мин. пауза между попытками молнии (тики)."))
    public int lightningIntervalMinTicks = 320;

    @Comment(@CommentValue("Макс. пауза между попытками молнии (тики)."))
    public int lightningIntervalMaxTicks = 720;

    @Comment(@CommentValue("Шанс молнии при каждой попытке (0–1)."))
    public double lightningChance = 0.38;

    @Comment(@CommentValue("Горизонтальный разброс удара от жерла (блоки)."))
    public double lightningRadiusBlocks = 14.0;

    @Comment(@CommentValue("Высота точки молнии над жерлом (блоки)."))
    public double lightningHeightBlocks = 32.0;
}
