package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Настройки типа аирдропа. Лут сундука — loot/<lootTableId>.yml"))
public final class AirDropTypeSettings extends YamlSerializable {

    @Comment(@CommentValue("Ключ lang/ для названия типа."))
    public String displayNameKey = "airdrop.types.default.name";

    @Comment(@CommentValue("ID таблицы лута в loot/ (по умолчанию = id файла)."))
    public String lootTableId = "";

    @Comment(@CommentValue("Автоспавн по таймеру."))
    public boolean enabled = true;

    @Comment(@CommentValue("Минут между автоспавнами. 0 = только ручной."))
    public long intervalMinutes = 45L;

    @Comment(@CommentValue("Макс. одновременно активных этого типа."))
    public int maxConcurrent = 1;

    @Comment(@CommentValue("Миры и WorldGuard."))
    public WorldPlacementSettings worldPlacement = new WorldPlacementSettings();

    @Comment(@CommentValue("Случайные координаты спавна."))
    public RandomSpawnSettings randomSpawn = new RandomSpawnSettings();

    @Comment(@CommentValue("Сообщения в чат по этапам аирдропа."))
    public BroadcastSettings broadcast = new BroadcastSettings();

    @Comment(@CommentValue("Ручной призыв и стоимость."))
    public SummonSettings summon = new SummonSettings();

    @Comment(@CommentValue("Профиль gate из protection.yml ядра SoulEvents."))
    public String gateProfileId = "default";

    @Comment(@CommentValue("Радиус защиты арены (блоки от сундука)."))
    public int arenaRadius = 24;

    @Comment(@CommentValue("ID схематики в папке schematics/ ядра."))
    public String schematicId = "";

    @Comment(@CommentValue("Плавное сглаживание краёв схематики."))
    public boolean landscapeBlend = true;

    @Comment(@CommentValue("Радиус сглаживания схематики."))
    public int blendRadius = 4;

    @Comment(@CommentValue("Фаза «beacon смерти» перед открытием."))
    public PreOpenBeaconSettings preOpenBeacon = new PreOpenBeaconSettings();

    @Comment(@CommentValue("Волна мобов перед открытием."))
    public PreOpenMobsSettings preOpenMobs = new PreOpenMobsSettings();

    @Comment(@CommentValue("Особый предмет для открытия сундука."))
    public RequiredLootSettings requiredLoot = new RequiredLootSettings();

    @Comment(@CommentValue("Эффекты и голограмма при спавне."))
    public VisualSettings visual = new VisualSettings();

    @Comment(@CommentValue("Кластер из 4 сундуков вокруг точки спавна."))
    public ChestClusterSettings chestCluster = ChestClusterSettings.defaults();

    @Comment(@CommentValue("Временный WG-регион арены (PvP, ломать, TNT — всё разрешено)."))
    public ArenaWorldGuardSettings arenaWorldGuard = new ArenaWorldGuardSettings();

    @Comment(@CommentValue("Очистка после открытия сундука."))
    public LifecycleSettings lifecycle = new LifecycleSettings();
}
