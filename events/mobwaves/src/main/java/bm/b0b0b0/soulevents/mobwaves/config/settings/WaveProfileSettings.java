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

    @Comment(@CommentValue("Эффекты для каждого моба орды. У типа в mob-overrides можно добавить свои."))
    public List<MobPotionEffectSettings> defaultMobEffects = defaultMobEffects();

    public static List<MobPotionEffectSettings> defaultMobEffects() {
        List<MobPotionEffectSettings> effects = new ArrayList<>();
        MobPotionEffectSettings fireResistance = new MobPotionEffectSettings();
        fireResistance.type = "FIRE_RESISTANCE";
        fireResistance.amplifier = 0;
        fireResistance.durationTicks = -1;
        effects.add(fireResistance);
        return effects;
    }

    public static List<WaveDefinitionSettings> defaultWaves() {
        List<WaveDefinitionSettings> waves = new ArrayList<>();

        WaveDefinitionSettings wave1 = new WaveDefinitionSettings();
        wave1.name = "Разведка";
        wave1.entries = waveOneDefaultEntries();
        wave1.superBossEnabled = true;
        wave1.superBoss = WaveMobEntrySettings.of("ZOMBIE", 1, 80.0);
        waves.add(wave1);

        WaveDefinitionSettings wave2 = new WaveDefinitionSettings();
        wave2.name = "Натиск";
        wave2.entries = new ArrayList<>();
        wave2.entries.add(WaveMobEntrySettings.of("ZOMBIE", 3));
        wave2.entries.add(WaveMobEntrySettings.of("SKELETON", 2));
        wave2.entries.add(WaveMobEntrySettings.of("HUSK", 1));
        wave2.superBossEnabled = true;
        wave2.superBoss = WaveMobEntrySettings.of("HUSK", 1, 120.0);
        waves.add(wave2);

        WaveDefinitionSettings wave3 = new WaveDefinitionSettings();
        wave3.name = "Эскалация";
        wave3.entries = new ArrayList<>();
        wave3.entries.add(WaveMobEntrySettings.of("ZOMBIE", 4));
        wave3.entries.add(WaveMobEntrySettings.of("SKELETON", 4));
        wave3.entries.add(WaveMobEntrySettings.of("HUSK", 3));
        wave3.superBossEnabled = true;
        wave3.superBoss = WaveMobEntrySettings.of("STRAY", 1, 180.0);
        waves.add(wave3);

        WaveDefinitionSettings wave4 = new WaveDefinitionSettings();
        wave4.name = "Финал";
        wave4.entries = new ArrayList<>();
        wave4.entries.add(WaveMobEntrySettings.of("HUSK", 5));
        wave4.entries.add(WaveMobEntrySettings.of("STRAY", 4));
        wave4.entries.add(WaveMobEntrySettings.of("SKELETON", 3));
        wave4.superBossEnabled = true;
        wave4.superBoss = WaveMobEntrySettings.of("ZOMBIE", 1, 260.0);
        waves.add(wave4);

        return waves;
    }

    public static List<WaveMobEntrySettings> waveOneDefaultEntries() {
        List<WaveMobEntrySettings> entries = new ArrayList<>();
        entries.add(WaveMobEntrySettings.of("ZOMBIE", 3));
        entries.add(WaveMobEntrySettings.of("SKELETON", 2));
        return entries;
    }

    public static Map<String, MobTypeOverrideSettings> defaultMobOverrides() {
        Map<String, MobTypeOverrideSettings> overrides = new HashMap<>();
        MobTypeOverrideSettings zombie = new MobTypeOverrideSettings();
        zombie.maxHealth = 80.0;
        zombie.damageMultiplier = 2.0;
        zombie.speedMultiplier = 1.08;
        overrides.put("ZOMBIE", zombie);
        MobTypeOverrideSettings skeleton = new MobTypeOverrideSettings();
        skeleton.maxHealth = 65.0;
        skeleton.damageMultiplier = 1.8;
        skeleton.speedMultiplier = 1.05;
        overrides.put("SKELETON", skeleton);
        MobTypeOverrideSettings husk = new MobTypeOverrideSettings();
        husk.maxHealth = 95.0;
        husk.damageMultiplier = 2.2;
        husk.speedMultiplier = 1.08;
        overrides.put("HUSK", husk);
        MobTypeOverrideSettings stray = new MobTypeOverrideSettings();
        stray.maxHealth = 70.0;
        stray.damageMultiplier = 1.9;
        stray.speedMultiplier = 1.05;
        overrides.put("STRAY", stray);
        return overrides;
    }
}
