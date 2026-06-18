package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class ArenaWorldGuardSettings extends YamlSerializable {

    @Comment(@CommentValue("Создавать временный WG-регион на время ивента (перебивает чужие флаги)."))
    public boolean createTempRegion = true;

    @Comment(@CommentValue("Приоритет региона (выше = сильнее перекрывает другие)."))
    public int regionPriority = 1000;

    @Comment(@CommentValue("Вертикальный радиус кубоида (блоки вверх/вниз от сундука)."))
    public int verticalRadius = 48;
}
