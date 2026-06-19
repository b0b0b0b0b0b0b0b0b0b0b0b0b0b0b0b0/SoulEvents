package bm.b0b0b0.soulevents.airdrop.database;

import bm.b0b0b0.soulevents.airdrop.config.DatabaseConfig;
import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SchemaMigrator {

    private final JavaPlugin plugin;
    private final DataSource dataSource;
    private final DatabaseConfig.StorageType storageType;

    public SchemaMigrator(JavaPlugin plugin, DataSource dataSource, DatabaseConfig.StorageType storageType) {
        this.plugin = plugin;
        this.dataSource = dataSource;
        this.storageType = storageType;
    }

    public void migrate() throws SQLException, IOException {
        if (isAlreadyMigrated()) {
            return;
        }
        runScript("database/001_airdrop.sql");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO schema_version(version) VALUES (1)");
        }
    }

    private boolean isAlreadyMigrated() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT version FROM schema_version")) {
            return resultSet.next();
        } catch (SQLException exception) {
            return false;
        }
    }

    private void runScript(String resourcePath) throws SQLException, IOException {
        List<String> statements = readStatements(resourcePath);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.executeUpdate(translateDialect(sql));
            }
        }
    }

    private String translateDialect(String sql) {
        if (storageType != DatabaseConfig.StorageType.MYSQL) {
            return sql;
        }
        return sql
                .replace("AUTOINCREMENT", "AUTO_INCREMENT")
                .replace("CREATE INDEX IF NOT EXISTS", "CREATE INDEX");
    }

    private List<String> readStatements(String resourcePath) throws IOException {
        InputStream stream = plugin.getResource(resourcePath);
        if (stream == null) {
            throw new IOException("Missing migration: " + resourcePath);
        }
        List<String> statements = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                builder.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    statements.add(builder.toString().trim());
                    builder.setLength(0);
                }
            }
        }
        return statements;
    }
}
