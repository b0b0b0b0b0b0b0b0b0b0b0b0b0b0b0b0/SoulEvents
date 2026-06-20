package bm.b0b0b0.soulevents.mobwaves.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class HordeSchematicMarkerSettings extends YamlSerializable {

    @Comment(@CommentValue("Сколько bedrock-маркеров активировать за один спавн (случайный выбор без повторов). Не больше, чем маркеров в .schem."))
    public int spawnCount = 1;

    @Comment(@CommentValue("true = bedrock-маркер заменяется воздухом (точка выхода орды)."))
    public boolean replaceWithAir = true;
}
