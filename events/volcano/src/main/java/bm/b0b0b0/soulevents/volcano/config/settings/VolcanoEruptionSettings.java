package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class VolcanoEruptionSettings {

    @Comment(@CommentValue("Фиксированный пул лута (только если sustainUntilExtinguish: false)."))
    public int itemCount = 12;

    @Comment(@CommentValue("true = долив по мере подбора до extinguish; false = один раз itemCount и стоп."))
    public boolean sustainUntilExtinguish = true;

    @Comment(@CommentValue("Держать столько неподобранных в радиусе; долив, когда меньше (4 = после подбора 1 из 4 снова пуляем)."))
    public int refillWhenUnclaimedBelow = 4;

    @Comment(@CommentValue("Мин. предметов за один долив в очередь."))
    public int refillBatchSizeMin = 1;

    @Comment(@CommentValue("Макс. предметов за один долив в очередь."))
    public int refillBatchSizeMax = 2;

    @Comment(@CommentValue("Мин. предметов в первой очереди при старте извержения."))
    public int initialBurstMin = 3;

    @Comment(@CommentValue("Макс. предметов в первой очереди при старте извержения."))
    public int initialBurstMax = 4;

    @Comment(@CommentValue("Мин. пауза между вылетами предметов (тики)."))
    public int minTicksBetweenLaunches = 18;

    @Comment(@CommentValue("Макс. пауза между вылетами предметов (тики)."))
    public int maxTicksBetweenLaunches = 55;

    @Comment(@CommentValue("Мин. пауза после подбора перед следующим вылетом (тики, 20 = 1 сек)."))
    public int pickupRefillDelayMinTicks = 60;

    @Comment(@CommentValue("Макс. пауза после подбора перед следующим вылетом (тики)."))
    public int pickupRefillDelayMaxTicks = 100;

    @Comment(@CommentValue("Макс. неподобранного лута вокруг жерла (0 = без лимита)."))
    public int maxUnclaimedAroundVent = 6;

    @Comment(@CommentValue("Радиус учёта лежащего лута от жерла (блоки)."))
    public double lootTrackRadiusBlocks = 14.0;

    @Comment(@CommentValue("Пауза перед повторной проверкой лимита лута (тики)."))
    public int lootCapacityRetryTicks = 12;

    @Comment(@CommentValue("Мин. угол между соседними вылетами (градусы), чтобы не летели в одну сторону."))
    public int launchAngleMinSeparationDegrees = 42;

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

    @Comment(@CommentValue("Катиться вниз по склону после приземления."))
    public boolean lootSlopeRollEnabled = true;

    @Comment(@CommentValue("Проверка ската раз в N тиков на предмет."))
    public int lootSlopeRollIntervalTicks = 2;

    @Comment(@CommentValue("Мин. перепад высоты соседней колонки (блоки), чтобы толкнуть вниз."))
    public int lootSlopeRollMinDrop = 1;

    @Comment(@CommentValue("Горизонтальная скорость подталкивания по склону."))
    public double lootSlopeRollPush = 0.10;

    @Comment(@CommentValue("Макс. тиков ската на предмет (0 = без лимита)."))
    public int lootSlopeRollMaxTicks = 90;

    @Comment(@CommentValue("Не выпускать лут, пока нет игроков в радиусе (как BossBar)."))
    public boolean requirePlayersForLoot = true;

    @Comment(@CommentValue("Радиус игроков для лута; 0 = lifecycle.bossBarRadius."))
    public int requirePlayerRadiusBlocks = 0;

    @Comment(@CommentValue("Задержка подбора после вылета (тики, 40 ≈ 2 сек). 0 = сразу."))
    public int lootLaunchPickupDelayTicks = 50;
}
