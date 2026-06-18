package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class MysqlSettings {

    @Comment(@CommentValue("Хост MySQL."))
    public String host = "127.0.0.1";

    @Comment(@CommentValue("Порт MySQL."))
    public int port = 3306;

    @Comment(@CommentValue("Имя базы данных."))
    public String database = "soulevents";

    @Comment(@CommentValue("Логин."))
    public String username = "root";

    @Comment(@CommentValue("Пароль."))
    public String password = "";
}
