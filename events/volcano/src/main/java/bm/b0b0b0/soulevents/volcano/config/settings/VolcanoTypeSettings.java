package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Настройки типа вулкана. Лут — loot/<lootTableId>.yml"))
public final class VolcanoTypeSettings extends YamlSerializable {

    @Comment(@CommentValue("Ключ lang/ для названия типа."))
    public String displayNameKey = "volcano.types.default.name";

    @Comment(@CommentValue("ID таблицы лута в loot/ (по умолчанию = id файла)."))
    public String lootTableId = "";

    @Comment(@CommentValue("Автоспавн по таймеру."))
    public boolean enabled = true;

    @Comment(@CommentValue("Минут между автоспавнами. 0 = только ручной."))
    public long intervalMinutes = 45L;

    @Comment(@CommentValue("Макс. одновременно активных этого типа."))
    public int maxConcurrent = 1;

    public WorldPlacementSettings worldPlacement = new WorldPlacementSettings();

    public RandomSpawnSettings randomSpawn = new RandomSpawnSettings();

    public BroadcastSettings broadcast = new BroadcastSettings();

    public SummonSettings summon = new SummonSettings();

    @Comment(@CommentValue("Профиль gate из protection.yml ядра SoulEvents."))
    public String gateProfileId = "default";

    @Comment(@CommentValue("Схематика вулкана (bedrock = жерло извержения)."))
    public SchematicTypeSettings schematic = new SchematicTypeSettings();

    @Comment(@CommentValue("Дым, подписи предметов, bossbar."))
    public VolcanoVisualSettings visual = new VolcanoVisualSettings();

    @Comment(@CommentValue("Параметры вылета предметов из жерла."))
    public VolcanoEruptionSettings eruption = new VolcanoEruptionSettings();

    @Comment(@CommentValue("Временный WG-регион вокруг арены."))
    public ArenaWorldGuardSettings arenaWorldGuard = new ArenaWorldGuardSettings();

    @Comment(@CommentValue("Таймеры: задержка извержения, потухание."))
    public VolcanoLifecycleSettings lifecycle = new VolcanoLifecycleSettings();

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
