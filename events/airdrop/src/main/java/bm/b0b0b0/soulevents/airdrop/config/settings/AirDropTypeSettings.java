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

    @Comment(@CommentValue("Устарело: используйте arenaWorldGuard.marginWithoutSchematic."))
    public int arenaRadius = 50;

    @Comment(@CommentValue("Схематика: id файла + placement/paste/blend для этого типа."))
    public SchematicTypeSettings schematic = new SchematicTypeSettings();

    @Comment(@CommentValue("Фаза «beacon смерти» перед открытием."))
    public PreOpenBeaconSettings preOpenBeacon = new PreOpenBeaconSettings();

    @Comment(@CommentValue("Wave-defense через SoulEvents-MobWaves — см. комментарии в секции."))
    public WaveDefenseSettings waveDefense = new WaveDefenseSettings();

    @Comment(@CommentValue("Право на открытие сундука."))
    public OpenPermissionSettings openPermission = new OpenPermissionSettings();

    @Comment(@CommentValue("Кастомный предмет в руке для открытия сундука."))
    public RequiredLootSettings requiredLoot = new RequiredLootSettings();

    @Comment(@CommentValue("Эффекты и голограмма при спавне."))
    public VisualSettings visual = new VisualSettings();

    @Comment(@CommentValue("Кластер из 4 сундуков вокруг точки спавна."))
    public ChestClusterSettings chestCluster = ChestClusterSettings.defaults();

    @Comment(@CommentValue("Временный WG-регион: все флаги ALLOW, deny — взрывы/жидкости (см. arenaWorldGuard)."))
    public ArenaWorldGuardSettings arenaWorldGuard = new ArenaWorldGuardSettings();

    @Comment(@CommentValue("Очистка после открытия сундука."))
    public LifecycleSettings lifecycle = new LifecycleSettings();

    public boolean usesSchematic() {
        return schematic.enabled && schematic.id != null && !schematic.id.isBlank();
    }

    public String schematicId() {
        if (!usesSchematic()) {
            return "";
        }
        return schematic.id.trim();
    }
}
