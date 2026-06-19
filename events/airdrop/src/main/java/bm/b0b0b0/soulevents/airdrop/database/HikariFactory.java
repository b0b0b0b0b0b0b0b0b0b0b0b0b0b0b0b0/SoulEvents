package bm.b0b0b0.soulevents.airdrop.database;

import bm.b0b0b0.soulevents.airdrop.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class HikariFactory {

    private HikariFactory() {
    }

    public static HikariDataSource create(JavaPlugin plugin, DatabaseConfig databaseConfig) {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(databaseConfig.poolConfig().maximumPoolSize);
        config.setMinimumIdle(databaseConfig.poolConfig().minimumIdle);
        config.setConnectionTimeout(databaseConfig.poolConfig().connectionTimeoutMs);
        config.setIdleTimeout(databaseConfig.poolConfig().idleTimeoutMs);
        config.setMaxLifetime(databaseConfig.poolConfig().maxLifetimeMs);
        if (databaseConfig.storageType() == DatabaseConfig.StorageType.MYSQL) {
            config.setJdbcUrl("jdbc:mysql://" + databaseConfig.mysqlHost() + ":" + databaseConfig.mysqlPort()
                    + "/" + databaseConfig.mysqlDatabase() + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
            config.setUsername(databaseConfig.mysqlUsername());
            config.setPassword(databaseConfig.mysqlPassword());
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            return new HikariDataSource(config);
        }
        File storage = new File(plugin.getDataFolder(), databaseConfig.storageDirectory());
        if (!storage.isDirectory() && !storage.mkdirs()) {
            throw new IllegalStateException("Could not create SQLite storage directory: " + storage.getAbsolutePath());
        }
        File databaseFile = new File(storage, databaseConfig.sqliteFileName());
        config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");
        return new HikariDataSource(config);
    }
}
