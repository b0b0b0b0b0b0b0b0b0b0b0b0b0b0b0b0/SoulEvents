package bm.b0b0b0.soulevents.core.stats;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import bm.b0b0b0.soulevents.core.config.settings.StatsSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class StatsRepository {

    private static final String UPSERT = """
            INSERT INTO player_stats(player_uuid, module_id, scope_id, metric, value)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, module_id, scope_id, metric)
            DO UPDATE SET value = player_stats.value + excluded.value
            """;

    private static final String SELECT_PLAYER = """
            SELECT module_id, scope_id, metric, value
            FROM player_stats
            WHERE player_uuid = ?
            """;

    private static final String SELECT_VALUE = """
            SELECT value
            FROM player_stats
            WHERE player_uuid = ? AND module_id = ? AND scope_id = ? AND metric = ?
            """;

    private final HikariDataSource dataSource;

    StatsRepository(JavaPlugin plugin, StatsSettings settings) throws SQLException, IOException {
        HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(Math.max(1, settings.maximumPoolSize));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(10_000L);
        config.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve(settings.sqliteFileName));
        config.setDriverClassName("org.sqlite.JDBC");
        config.setPoolName("SoulEvents-Stats");
        this.dataSource = new HikariDataSource(config);
        migrate(plugin);
    }

    void flush(Map<PlayerStatKey, Long> deltas) throws SQLException {
        if (deltas.isEmpty()) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT)) {
                for (Map.Entry<PlayerStatKey, Long> entry : deltas.entrySet()) {
                    if (entry.getValue() == 0L) {
                        continue;
                    }
                    PlayerStatKey key = entry.getKey();
                    statement.setString(1, key.playerId().toString());
                    statement.setString(2, key.moduleId());
                    statement.setString(3, key.scopeId());
                    statement.setString(4, key.metric());
                    statement.setLong(5, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        }
    }

    Map<PlayerStatKey, Long> loadPlayer(UUID playerId) throws SQLException {
        Map<PlayerStatKey, Long> loaded = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_PLAYER)) {
            statement.setString(1, playerId.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    loaded.put(
                            new PlayerStatKey(
                                    playerId,
                                    resultSet.getString("module_id"),
                                    resultSet.getString("scope_id"),
                                    resultSet.getString("metric")
                            ),
                            resultSet.getLong("value")
                    );
                }
            }
        }
        return loaded;
    }

    long loadValue(PlayerStatKey key) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_VALUE)) {
            statement.setString(1, key.playerId().toString());
            statement.setString(2, key.moduleId());
            statement.setString(3, key.scopeId());
            statement.setString(4, key.metric());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("value");
                }
            }
        }
        return 0L;
    }

    void close() {
        dataSource.close();
    }

    private void migrate(JavaPlugin plugin) throws SQLException, IOException {
        for (String sql : readStatements(plugin, "database/001_stats.sql")) {
            try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
                statement.executeUpdate(sql);
            }
        }
    }

    private static List<String> readStatements(JavaPlugin plugin, String resourcePath) throws IOException {
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
