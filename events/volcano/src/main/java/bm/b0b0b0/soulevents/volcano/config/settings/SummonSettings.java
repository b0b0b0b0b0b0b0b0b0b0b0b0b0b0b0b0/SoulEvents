package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public class SummonSettings extends YamlSerializable {

    @Comment(@CommentValue("Игроки могут призывать этот тип (/airdrop summon <тип>)."))
    public boolean playerSummonEnabled = true;

    @Comment(@CommentValue("Админ может принудительно заспавнить (/airdrop admin, GUI)."))
    public boolean adminSummonEnabled = true;

    @Comment(@CommentValue("Стоимость призыва через Vault. 0 = бесплатно."))
    public double vaultCost = 0.0;
}
