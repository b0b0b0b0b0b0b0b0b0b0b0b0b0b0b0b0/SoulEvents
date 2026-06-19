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

public final class SchemaMigrator {

    private final JavaPlugin plugin;
    private final DataSource dataSource;

    public SchemaMigrator(JavaPlugin plugin, DataSource dataSource) {
        this.plugin = plugin;
        this.dataSource = dataSource;
    }

    public void migrate() throws SQLException, IOException {
        runScript("database/001_airdrop.sql");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO schema_version(version) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM schema_version)"
            );
        }
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
