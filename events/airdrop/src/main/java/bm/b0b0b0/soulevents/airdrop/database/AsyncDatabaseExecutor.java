package bm.b0b0b0.soulevents.airdrop.database;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class AsyncDatabaseExecutor {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SoulEvents-AirDrop-DB");
        thread.setDaemon(true);
        return thread;
    });

    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> run(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public void shutdown() {
        executor.shutdown();
    }
}
