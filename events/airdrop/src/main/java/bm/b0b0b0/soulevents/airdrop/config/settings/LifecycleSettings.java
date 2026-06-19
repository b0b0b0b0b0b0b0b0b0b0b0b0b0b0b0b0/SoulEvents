package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class LifecycleSettings extends YamlSerializable {

    @Comment(@CommentValue("Мин. секунд до открытия сундука (всегда, даже для default)."))
    public int minOpenDelaySeconds = 60;

    @Comment(@CommentValue("Секунд до удаления аirdrop после полного опустошения сундука."))
    public int cleanupSecondsAfterLooted = 15;

    @Comment(@CommentValue("Макс. секунд с момента открытия сундука — потом принудительное удаление (лут внутри сгорает)."))
    public int maxActiveSecondsAfterLootable = 240;

    @Comment(@CommentValue("BossBar игрокам в радиусе от якоря сундука (0 = выкл.)."))
    public int bossBarRadius = 200;

    @Comment(@CommentValue("Показывать BossBar с таймером исчезновения."))
    public boolean bossBarEnabled = true;

    @Comment(@CommentValue("Lang-ключ BossBar до открытия сундука (ожидание)."))
    public String bossBarWaitingKey = "airdrop.bossbar.waiting";

    @Comment(@CommentValue("Lang-ключ BossBar, пока в сундуке есть лут."))
    public String bossBarDespawnKey = "airdrop.bossbar.despawn";

    @Comment(@CommentValue("Lang-ключ BossBar после полного опустошения."))
    public String bossBarDespawnLootedKey = "airdrop.bossbar.despawn-looted";
}
