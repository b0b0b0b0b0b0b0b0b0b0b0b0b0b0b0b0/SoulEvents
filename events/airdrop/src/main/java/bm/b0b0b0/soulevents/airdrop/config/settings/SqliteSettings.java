package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class SqliteSettings {

    @Comment(@CommentValue("Имя файла SQLite"))
    public String fileName = "airdrop.db";
}
