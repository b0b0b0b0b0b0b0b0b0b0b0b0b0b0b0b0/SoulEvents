package bm.b0b0b0.soulevents.volcano.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class TypeSchematicMarkerSettings extends YamlSerializable {

    @Comment(@CommentValue("Сколько bedrock-маркеров активировать за один спавн (случайный выбор без повторов). Не больше, чем маркеров в .schem."))
    public int spawnCount = 1;

    @Comment(@CommentValue("false = bedrock-маркер остаётся в мире (дно жерла под лавой). Airdrop по умолчанию true."))
    public boolean replaceWithAir = false;
}
