package bm.b0b0b0.soulevents.airdrop.config.settings;

import net.elytrium.serializer.annotations.Comment;
import net.elytrium.serializer.annotations.CommentValue;

public final class DatabasePoolSettings {

    @Comment(@CommentValue("Макс. соединений в пуле HikariCP."))
    public int maximumPoolSize = 4;

    @Comment(@CommentValue("Мин. idle-соединений."))
    public int minimumIdle = 1;

    @Comment(@CommentValue("Таймаут получения соединения (мс)."))
    public long connectionTimeoutMs = 10_000L;

    @Comment(@CommentValue("Таймаут простоя соединения (мс)."))
    public long idleTimeoutMs = 600_000L;

    @Comment(@CommentValue("Макс. время жизни соединения (мс)."))
    public long maxLifetimeMs = 1_800_000L;
}
