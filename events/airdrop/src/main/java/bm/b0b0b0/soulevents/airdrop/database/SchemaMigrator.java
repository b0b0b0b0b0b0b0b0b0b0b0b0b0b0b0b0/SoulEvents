package bm.b0b0b0.soulevents.airdrop.database;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SchemaMigrator {

    private final JavaPlugin plugin;
    private final DataSource dataSource;

    public SchemaMigrator(JavaPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    public void migrate() throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            if (isMySql(connection)) {
                runScript("database/001_airdrop_mysql.sql");
                ensureIndex(connection, "airdrop_sessions", "idx_airdrop_sessions_type", "type_id");
            } else {
                runScript("database/001_airdrop.sql");
            }
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(
                        "INSERT INTO schema_version(version) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM schema_version)"
                );
            }
        }
    }

    private void ensureIndex(Connection connection, String table, String indexName, String column)
            throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE INDEX " + indexName + " ON " + table + "(" + column + ")");
        } catch (SQLException exception) {
            if (!isDuplicateIndex(exception)) {
                throw exception;
            }
        }
    }

    private static boolean isDuplicateIndex(SQLException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("duplicate key name") || lower.contains("already exists");
    }

    private static boolean isMySql(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName();
        return product != null && product.toLowerCase(Locale.ROOT).contains("mysql");
    }

    private void runScript(String resourcePath) throws SQLException, IOException {
        List<String> statements = readStatements(resourcePath);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.executeUpdate(sql);
            }
        }
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
