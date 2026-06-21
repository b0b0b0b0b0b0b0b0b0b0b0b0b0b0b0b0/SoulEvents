package bm.b0b0b0.soulevents.core.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.language.object.YamlSerializable;

public final class StatsSettings extends YamlSerializable {

    @Comment(@CommentValue("Сбор статистики игроков (убийства, лут, сундуки)."))
    public boolean enabled = true;

    @Comment(@CommentValue("Интервал сброса буфера в SQLite (сек)."))
    public int flushIntervalSeconds = 30;

    @Comment(@CommentValue("Файл SQLite в папке плагина."))
    public String sqliteFileName = "stats.db";

    @Comment(@CommentValue("Размер пула HikariCP."))
    public int maximumPoolSize = 2;
}
