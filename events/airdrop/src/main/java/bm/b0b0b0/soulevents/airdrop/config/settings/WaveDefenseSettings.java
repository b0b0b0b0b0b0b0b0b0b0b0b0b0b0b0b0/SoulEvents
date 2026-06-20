package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class WaveDefenseSettings extends YamlSerializable {

    @Comment(@CommentValue("Волны мобов из SoulEvents-MobWaves перед/во время открытия лута."))
    public boolean enabled = false;

    @Comment(@CommentValue("ID профиля в mobwaves/profiles/<id>.yml"))
    public String profileId = "default";

    @Comment(@CommentValue("Радиус спавна вокруг anchor. 0 = из профиля MobWaves."))
    public int spawnRadius = 0;
}
