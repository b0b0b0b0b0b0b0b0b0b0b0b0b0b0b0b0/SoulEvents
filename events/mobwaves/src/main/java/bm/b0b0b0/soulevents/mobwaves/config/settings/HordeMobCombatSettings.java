package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeMobCombatSettings extends YamlSerializable {

    @Comment(@CommentValue("Множитель max HP, если в профиле для типа max-health = 0."))
    public double healthMultiplier = 5.0;

    @Comment(@CommentValue("Множитель урона атаки, если в профиле damage-multiplier = 1."))
    public double damageMultiplier = 5.0;

    @Comment(@CommentValue("Множитель скорости, если в профиле speed-multiplier = 1."))
    public double speedMultiplier = 1.2;

    @Comment(@CommentValue("Множитель HP супербосса волны (к базовому HP моба)."))
    public double superBossHealthMultiplier = 5.0;

    @Comment(@CommentValue("Бонус к GENERIC_ARMOR (поверх ваниллы/брони на мобе)."))
    public double armorBonus = 6.0;

    @Comment(@CommentValue("Бонус к GENERIC_ARMOR_TOUGHNESS."))
    public double armorToughnessBonus = 3.0;

    @Comment(@CommentValue("GENERIC_KNOCKBACK_RESISTANCE (0–1). 0.85 = почти не откидываются."))
    public double knockbackResistance = 0.85;

    @Comment(@CommentValue("Зомби/скелеты и пр. не горят на солнце."))
    public boolean immuneToSunlight = true;

    @Comment(@CommentValue("Надеть полный сет брони (визуал + защита)."))
    public boolean equipArmor = true;

    @Comment(@CommentValue("Материал сета: IRON, DIAMOND, NETHERITE…"))
    public String armorMaterial = "NETHERITE";

    @Comment(@CommentValue("Оружие в руку (меч/лук с зачарованиями)."))
    public boolean equipWeapons = true;

    @Comment(@CommentValue("Принудительно выбирать ближайшего игрока целью (не биться между собой)."))
    public boolean forceTargetPlayers = true;

    @Comment(@CommentValue("Радиус поиска цели-игрока для моба орды."))
    public int targetRadiusBlocks = 64;

    @Comment(@CommentValue("Бонус к GENERIC_FOLLOW_RANGE (видят игроков издалека)."))
    public double followRangeBonus = 48.0;

    @Comment(@CommentValue("Убрать ванильный дроп и опыт — остаётся только лут плагина с моба."))
    public boolean clearVanillaDrops = true;

    @Comment(@CommentValue("Не уводить мобов далеко от обелиска (якоря орды)."))
    public boolean keepNearAnchor = true;

    @Comment(@CommentValue("Преследовать игрока только если он в этом радиусе от якоря. 0 = spawn-radius + 24."))
    public int maxChaseDistanceFromAnchorBlocks = 0;

    @Comment(@CommentValue("Если моб дальше — сначала тянуть к якорю, затем «призыв» молнией и телепорт. 0 = spawn-radius + 12."))
    public int mobPullBackRadiusBlocks = 0;

    @Comment(@CommentValue("Скорость притягивания моба к разлому (pathfinder)."))
    public double mobPullSpeed = 1.35;

    @Comment(@CommentValue("Тиков подряд вне радиуса до «призыва» молнией (сервис каждые 10 тиков)."))
    public int mobRecallAttemptsBeforeLightning = 5;

    @Comment(@CommentValue("Частицы при притягивании моба к разлому."))
    public boolean mobPullParticlesEnabled = true;

    @Comment(@CommentValue("Белая подсветка (outline) обычных мобов орды."))
    public boolean mobOutlineEnabled = true;

    @Comment(@CommentValue("Красная подсветка супербоссов."))
    public boolean bossOutlineEnabled = true;

    @Comment(@CommentValue("Крупные частицы вокруг супербоссов."))
    public boolean bossParticlesEnabled = true;

    public int bossParticleCount = 24;

    public double bossParticleSpread = 1.2;

    @Comment(@CommentValue("Таймер волны: стартовое время до «просрочки»."))
    public int initialWaveTimerSeconds = 180;

    @Comment(@CommentValue("Сколько секунд добавляется к таймеру за каждого убитого моба."))
    public int secondsAddedPerKill = 180;

    public boolean waveTimerEnabled = true;
}
