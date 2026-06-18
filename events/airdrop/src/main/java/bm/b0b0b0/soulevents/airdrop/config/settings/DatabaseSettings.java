package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;
import net.elytrium.serializer.annotations.NewLine;

public final class DatabaseSettings {

    @Comment(@CommentValue("sqlite или mysql"))
    public String type = "sqlite";

    @Comment(@CommentValue("Отключить модуль при ошибке БД"))
    public boolean failOnConnect = true;

    @Comment(@CommentValue("Папка SQLite (относительно plugins/SoulEvents-AirDrop/)"))
    public String storageDirectory = "storage";

    @NewLine
    public SqliteSettings sqlite = new SqliteSettings();

    @NewLine
    public MysqlSettings mysql = new MysqlSettings();

    @NewLine
    public DatabasePoolSettings pool = new DatabasePoolSettings();
}
