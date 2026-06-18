package bm.b0b0b0.soulevents.airdrop.database;

import bm.b0b0b0.soulevents.airdrop.config.DatabaseConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class DatabaseBootstrap {

    private final JavaPlugin plugin;
    private final DatabaseConfig databaseConfig;
    private final DataSourceProvider dataSourceProvider;
    private final AsyncDatabaseExecutor asyncDatabaseExecutor;

    public DatabaseBootstrap(
            JavaPlugin plugin,
            DatabaseConfig databaseConfig,
            DataSourceProvider dataSourceProvider,
            AsyncDatabaseExecutor asyncDatabaseExecutor
    ) {
        this.plugin = plugin;
        this.databaseConfig = databaseConfig;
        this.dataSourceProvider = dataSourceProvider;
        this.asyncDatabaseExecutor = asyncDatabaseExecutor;
    }

    public CompletableFuture<Boolean> start() {
        return asyncDatabaseExecutor.supply(this::initialize);
    }

    public void shutdown() {
        dataSourceProvider.shutdown();
    }

    private boolean initialize() {
        try {
            HikariDataSource dataSource = HikariFactory.create(plugin, databaseConfig);
            new SchemaMigrator(plugin, dataSource).migrate();
            dataSourceProvider.assign(dataSource);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "AirDrop database init failed", exception);
            return !databaseConfig.failOnConnect();
        }
    }
}
