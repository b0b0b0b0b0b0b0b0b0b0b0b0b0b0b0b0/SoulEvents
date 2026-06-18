package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class PreOpenMobsSettings extends YamlSerializable {

    @Comment(@CommentValue("Волна мобов перед открытием сундука."))
    public boolean enabled = false;

    @Comment(@CommentValue("ID профиля волны мобов."))
    public String profileId = "default";

    @Comment(@CommentValue("Тот же gate-профиль, что и для открытия сундука (атакующие мобы)."))
    public String gateProfileId = "default";
}
