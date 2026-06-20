package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class WaveDefinitionSettings extends YamlSerializable {

    public String name = "Wave 1";
    public List<WaveMobEntrySettings> entries = defaultEntries();

    @Comment(@CommentValue("Супербосс волны — спавнится последним, HP × super-boss-health-multiplier из config."))
    public boolean superBossEnabled = true;

    public WaveMobEntrySettings superBoss = defaultSuperBoss();

    public static WaveMobEntrySettings defaultSuperBoss() {
        WaveMobEntrySettings boss = new WaveMobEntrySettings();
        boss.entityType = "ZOMBIE";
        boss.count = 1;
        return boss;
    }

    public static List<WaveMobEntrySettings> defaultEntries() {
        List<WaveMobEntrySettings> entries = new ArrayList<>();
        WaveMobEntrySettings zombie = new WaveMobEntrySettings();
        zombie.entityType = "ZOMBIE";
        zombie.count = 8;
        entries.add(zombie);
        WaveMobEntrySettings skeleton = new WaveMobEntrySettings();
        skeleton.entityType = "SKELETON";
        skeleton.count = 4;
        entries.add(skeleton);
        return entries;
    }
}
