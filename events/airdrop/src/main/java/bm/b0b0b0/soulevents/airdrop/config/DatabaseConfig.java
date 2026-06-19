package bm.b0b0b0.soulevents.airdrop.config;

import bm.b0b0b0.soulevents.airdrop.config.settings.DatabasePoolSettings;
import bm.b0b0b0.soulevents.airdrop.config.settings.DatabaseSettings;

public final class DatabaseConfig {

    private final DatabaseSettings settings;

    public DatabaseConfig(DatabaseSettings settings) {
        this.settings = settings;
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

    public DatabasePoolSettings poolConfig() {
        return settings.pool;
    }
}
