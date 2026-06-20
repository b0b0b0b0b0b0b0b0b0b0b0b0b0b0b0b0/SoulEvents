package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class VolcanoLifecycleSettings extends YamlSerializable {

    @Comment(@CommentValue("Секунд после paste до извержения лута."))
    public int eruptDelaySeconds = 8;

    @Comment(@CommentValue("Секунд активности вулкана до потухания (удаление + cleanup лута)."))
    public int maxActiveSeconds = 240;

    @Comment(@CommentValue("BossBar игрокам в радиусе от жерла (0 = выкл.)."))
    public int bossBarRadius = 48;

    public boolean bossBarEnabled = true;

    @Comment(@CommentValue("Lang: извержение через <timer>"))
    public String bossBarWaitingKey = "volcano.bossbar.waiting";

    @Comment(@CommentValue("Lang: Вулкан — Потухнет через: <timer>"))
    public String bossBarExtinguishKey = "volcano.bossbar.extinguish";
}
