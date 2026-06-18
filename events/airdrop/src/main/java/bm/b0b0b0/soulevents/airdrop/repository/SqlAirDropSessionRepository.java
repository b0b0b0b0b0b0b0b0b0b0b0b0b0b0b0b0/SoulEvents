package bm.b0b0b0.soulevents.airdrop.repository;

import bm.b0b0b0.soulevents.airdrop.database.AsyncDatabaseExecutor;
import bm.b0b0b0.soulevents.airdrop.database.DataSourceProvider;
import org.bukkit.Location;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class SqlAirDropSessionRepository {

    private final DataSourceProvider dataSourceProvider;
    private final AsyncDatabaseExecutor executor;

    public SqlAirDropSessionRepository(DataSourceProvider dataSourceProvider, AsyncDatabaseExecutor executor) {
        this.dataSourceProvider = dataSourceProvider;
        this.executor = executor;
    }

    public CompletableFuture<Void> insertSession(UUID sessionId, String typeId, Location anchor, String source) {
        return executor.run(() -> {
            try (Connection connection = dataSourceProvider.get().getConnection();
                 PreparedStatement session = connection.prepareStatement(
                         "INSERT INTO airdrop_sessions(session_id, type_id, world_name, x, y, z, phase, source, started_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                 );
                 PreparedStatement log = connection.prepareStatement(
                         "INSERT INTO airdrop_spawn_log(session_id, type_id, world_name, x, y, z, source, spawned_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
                 )) {
                long now = System.currentTimeMillis();
                session.setString(1, sessionId.toString());
                session.setString(2, typeId);
                session.setString(3, anchor.getWorld().getName());
                session.setInt(4, anchor.getBlockX());
                session.setInt(5, anchor.getBlockY());
                session.setInt(6, anchor.getBlockZ());
                session.setString(7, "PREPARING");
                session.setString(8, source);
                session.setLong(9, now);
                session.executeUpdate();

                log.setString(1, sessionId.toString());
                log.setString(2, typeId);
                log.setString(3, anchor.getWorld().getName());
                log.setInt(4, anchor.getBlockX());
                log.setInt(5, anchor.getBlockY());
                log.setInt(6, anchor.getBlockZ());
                log.setString(7, source);
                log.setLong(8, now);
                log.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }

    public CompletableFuture<Void> endSession(UUID sessionId, String phase) {
        return executor.run(() -> {
            try (Connection connection = dataSourceProvider.get().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "UPDATE airdrop_sessions SET phase = ?, ended_at = ? WHERE session_id = ?"
                 )) {
                statement.setString(1, phase);
                statement.setLong(2, System.currentTimeMillis());
                statement.setString(3, sessionId.toString());
                statement.executeUpdate();
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        });
    }
}
