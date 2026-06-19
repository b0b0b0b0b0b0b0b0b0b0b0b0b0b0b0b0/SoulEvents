package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class PreOpenBeaconSettings extends YamlSerializable {

    @Comment(@CommentValue("Фаза «beacon смерти» с дебаффами перед открытием."))
    public boolean enabled = false;

    @Comment(@CommentValue("Секунд до того, как сундук можно будет открыть."))
    public int delaySeconds = 60;

    @Comment(@CommentValue("Радиус «beacon смерти» (блоки). 0 = из профиля эффектов core."))
    public int radius = 0;

    @Comment(@CommentValue("ID профиля эффектов (ядро, protection.yml → effectProfiles)."))
    public String effectProfileId = "default";
}
