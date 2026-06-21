package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

@Comment({
        @CommentValue("Волны мобов — отдельный модуль SoulEvents-MobWaves (не часть airdrop)."),
        @CommentValue("Перед enabled: true положи SoulEvents-MobWaves.jar в plugins/ и настрой mobwaves/profiles/."),
        @CommentValue("Без MobWaves на сервере enabled игнорируется, сундук открывается как обычно.")
})
public final class WaveDefenseSettings extends YamlSerializable {

    @Comment(@CommentValue("true = волны до открытия сундука (требует SoulEvents-MobWaves)."))
    public boolean enabled = false;

    @Comment(@CommentValue("Профиль: plugins/SoulEvents-MobWaves/profiles/<id>.yml"))
    public String profileId = "default";

    @Comment(@CommentValue("Радиус спавна мобов вокруг anchor. 0 = из профиля MobWaves."))
    public int spawnRadius = 0;
}
