package bm.b0b0b0.soulevents.core.schematic;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public final class SchematicPasteQueue {

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "SoulEvents-SchematicPaste");
        thread.setDaemon(true);
        return thread;
    });

    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, executor);
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
