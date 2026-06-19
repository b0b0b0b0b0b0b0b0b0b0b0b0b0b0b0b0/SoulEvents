package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class VolcanoEruptionSettings {

    @Comment(@CommentValue("Сколько предметов выплёвывает за всё время активности вулкана."))
    public int itemCount = 12;

    @Comment(@CommentValue("Мин. пауза между вылетами предметов (тики)."))
    public int minTicksBetweenLaunches = 25;

    @Comment(@CommentValue("Макс. пауза между вылетами предметов (тики)."))
    public int maxTicksBetweenLaunches = 70;

    @Comment(@CommentValue("Мин. вертикальная скорость вылета (высота дуги)."))
    public double launchPowerMin = 0.58;

    @Comment(@CommentValue("Макс. вертикальная скорость вылета (высота дуги)."))
    public double launchPowerMax = 0.92;

    @Comment(@CommentValue("Мин. горизонтальная дальность вылета (блоки от жерла)."))
    public double horizontalReachMin = 5.0;

    @Comment(@CommentValue("Макс. горизонтальная дальность вылета (блоки от жерла)."))
    public double horizontalReachMax = 10.0;

    @Comment(@CommentValue("Делитель для перевода дальности в скорость (меньше = дальше)."))
    public double horizontalSpeedDivisor = 13.5;

    @Comment(@CommentValue("Базовая горизонтальная скорость поверх расчёта по дальности."))
    public double horizontalSpeedBias = 0.12;

    @Comment(@CommentValue("Частица хвоста за летящим предметом (Particle)."))
    public String trailParticle = "FLAME";

    @Comment(@CommentValue("Частиц хвоста за тик на предмет."))
    public int trailParticleCount = 6;

    @Comment(@CommentValue("Доп. частицы хвоста."))
    public String trailSecondaryParticle = "LAVA";

    public int trailSecondaryCount = 2;
}
