package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WaveProfileSettings extends YamlSerializable {

    @Comment(@CommentValue("Радиус спавна вокруг anchor. 0 = из config.yml модуля."))
    public int spawnRadius = 0;

    @Comment(@CommentValue("Мобов за один пакет. 0 = из config.yml."))
    public int batchSize = 0;

    @Comment(@CommentValue("Тиков между пакетами. 0 = из config.yml."))
    public int batchIntervalTicks = 0;

    @Comment(@CommentValue("Секунд открытого лута после волны. 0 = из config.yml."))
    public int graceAfterClearSeconds = 0;

    @Comment(@CommentValue("Волны по порядку."))
    public List<WaveDefinitionSettings> waves = defaultWaves();

    @Comment(@CommentValue("Переопределения по EntityType (кроме EVOKER и ENDER_DRAGON)."))
    public Map<String, MobTypeOverrideSettings> mobOverrides = defaultMobOverrides();

    public static List<WaveDefinitionSettings> defaultWaves() {
        List<WaveDefinitionSettings> waves = new ArrayList<>();
        waves.add(new WaveDefinitionSettings());
        return waves;
    }

    public static Map<String, MobTypeOverrideSettings> defaultMobOverrides() {
        Map<String, MobTypeOverrideSettings> overrides = new HashMap<>();
        MobTypeOverrideSettings zombie = new MobTypeOverrideSettings();
        zombie.maxHealth = 250.0;
        zombie.damageMultiplier = 5.0;
        zombie.speedMultiplier = 1.2;
        overrides.put("ZOMBIE", zombie);
        MobTypeOverrideSettings skeleton = new MobTypeOverrideSettings();
        skeleton.maxHealth = 200.0;
        skeleton.damageMultiplier = 4.5;
        skeleton.speedMultiplier = 1.15;
        overrides.put("SKELETON", skeleton);
        MobTypeOverrideSettings husk = new MobTypeOverrideSettings();
        husk.maxHealth = 280.0;
        husk.damageMultiplier = 5.0;
        husk.speedMultiplier = 1.15;
        overrides.put("HUSK", husk);
        MobTypeOverrideSettings stray = new MobTypeOverrideSettings();
        stray.maxHealth = 220.0;
        stray.damageMultiplier = 4.5;
        stray.speedMultiplier = 1.1;
        overrides.put("STRAY", stray);
        return overrides;
    }
}
