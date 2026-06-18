package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.DatabasePoolSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.DatabaseSettings;

public final class DatabaseConfig {

    public enum StorageType {
        SQLITE,
        MYSQL
    }

    private final DatabaseSettings settings;

    public DatabaseConfig(DatabaseSettings settings) {
        this.settings = settings;
    }

    public StorageType storageType() {
        if ("mysql".equalsIgnoreCase(settings.type)) {
            return StorageType.MYSQL;
        }
        return StorageType.SQLITE;
    }

    public boolean failOnConnect() {
        return settings.failOnConnect;
    }

    public String storageDirectory() {
        return settings.storageDirectory;
    }

    public String sqliteFileName() {
        return settings.sqlite.fileName;
    }

    public String mysqlHost() {
        return settings.mysql.host;
    }

    public int mysqlPort() {
        return settings.mysql.port;
    }

    public String mysqlDatabase() {
        return settings.mysql.database;
    }

    public String mysqlUsername() {
        return settings.mysql.username;
    }

    public String mysqlPassword() {
        return settings.mysql.password;
    }

    public DatabasePoolSettings poolConfig() {
        return settings.pool;
    }
}
