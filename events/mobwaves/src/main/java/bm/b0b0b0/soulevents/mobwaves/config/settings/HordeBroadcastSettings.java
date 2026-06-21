package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

import java.util.ArrayList;
import java.util.List;

public final class HordeBroadcastSettings extends YamlSerializable {

    public boolean enabled = true;
    public boolean spawnEnabled = true;
    public String messageKey = "mobwaves.broadcast.spawn";
    public boolean clearedEnabled = true;
    public String clearedMessageKey = "mobwaves.broadcast.cleared";
    public boolean mobKillActionBarEnabled = true;
    public List<String> mobKillActionBarKeys = defaultMobKillActionBarKeys();
    public boolean bossKillActionBarEnabled = true;
    public List<String> bossKillActionBarKeys = defaultBossKillActionBarKeys();
    public boolean removedEnabled = true;
    public String removedMessageKey = "mobwaves.broadcast.removed";

    private static List<String> defaultMobKillActionBarKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("mobwaves.actionbar.you-killed-mob.1");
        keys.add("mobwaves.actionbar.you-killed-mob.2");
        keys.add("mobwaves.actionbar.you-killed-mob.3");
        return keys;
    }

    private static List<String> defaultBossKillActionBarKeys() {
        List<String> keys = new ArrayList<>();
        keys.add("mobwaves.actionbar.you-killed-boss.1");
        keys.add("mobwaves.actionbar.you-killed-boss.2");
        keys.add("mobwaves.actionbar.you-killed-boss.3");
        return keys;
    }
}
