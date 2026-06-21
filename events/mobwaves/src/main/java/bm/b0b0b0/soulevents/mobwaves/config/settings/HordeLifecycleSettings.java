package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeLifecycleSettings extends YamlSerializable {

    @Comment(@CommentValue("Макс. секунд жизни орды от спавна (0 = без лимита до зачистки)."))
    public int maxActiveSeconds = 600;

    @Comment(@CommentValue("Секунд после зачистки всех волн, затем cleanup."))
    public int maxActiveSecondsAfterCleared = 30;

    @Comment(@CommentValue("BossBar игрокам в радиусе от нексуса (0 = выкл.)."))
    public int bossBarRadius = 48;

    public boolean bossBarEnabled = true;

    @Comment(@CommentValue("Не начинать волны, пока нет игрока в радиусе (нексус/эффекты уже на месте)."))
    public boolean requirePlayerForWaves = true;

    @Comment(@CommentValue("Радиус ожидания игрока для старта волн; 0 = bossBarRadius или 48."))
    public int requirePlayerRadiusBlocks = 48;

    @Comment(@CommentValue("Lang: ждём игрока для старта волн"))
    public String bossBarWaitingKey = "mobwaves.bossbar.waiting";

    @Comment(@CommentValue("Lang: волны активны, осталось <timer>"))
    public String bossBarActiveKey = "mobwaves.bossbar.active";

    @Comment(@CommentValue("Lang: орда исчезнет через <timer>"))
    public String bossBarDespawnKey = "mobwaves.bossbar.despawn";

    @Comment(@CommentValue("Lang: волна <wave>/<waves>, таймер <timer>, бонус <bonus> за убийство"))
    public String bossBarWaveKey = "mobwaves.bossbar.wave";

    @Comment(@CommentValue("Lang: босс волны <wave> повержен — жди <next_wave>, таймер <timer>"))
    public String bossBarWaveGraceKey = "mobwaves.bossbar.wave-grace";

    @Comment(@CommentValue("Lang: победа — разлом схлопывается, трофеи ещё <timer>"))
    public String bossBarVictoryKey = "mobwaves.bossbar.victory";
}
