package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment(@CommentValue("Тип самостоятельной орды (ивент MobWaves)."))
public final class HordeTypeSettings extends YamlSerializable {

    public String displayNameKey = "mobwaves.types.default.name";

    public String lootTableId = "";

    public boolean enabled = true;

    public long intervalMinutes = 60L;

    public int maxConcurrent = 1;

    @Comment(@CommentValue("Профиль волн из profiles/<id>.yml"))
    public String waveProfileId = "default";

    public int waveSpawnRadius = 0;

    public WorldPlacementSettings worldPlacement = new WorldPlacementSettings();

    public HordeRandomSpawnSettings randomSpawn = new HordeRandomSpawnSettings();

    public HordeBroadcastSettings broadcast = new HordeBroadcastSettings();

    public HordeMobLootSettings mobLoot = new HordeMobLootSettings();

    public HordeLootVisualSettings lootVisual = new HordeLootVisualSettings();

    public HordeLifecycleSettings lifecycle = new HordeLifecycleSettings();

    public HordeBuiltinNexusSettings builtinNexus = new HordeBuiltinNexusSettings();

    public HordeSchematicSettings schematic = new HordeSchematicSettings();

    public ArenaWorldGuardSettings arenaWorldGuard = new ArenaWorldGuardSettings();

    public boolean usesSchematic() {
        return schematic.enabled && schematic.id != null && !schematic.id.isBlank();
    }

    public String schematicId() {
        if (schematic.id == null || schematic.id.isBlank()) {
            return "horde_rift";
        }
        return schematic.id.trim();
    }

    public String structureNameKey() {
        if (usesSchematic()) {
            return schematic.structureNameKey;
        }
        return builtinNexus.structureNameKey;
    }
}
