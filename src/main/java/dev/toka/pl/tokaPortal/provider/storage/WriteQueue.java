package dev.toka.pl.tokaPortal.provider.storage;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.function.BiConsumer;

/** 單一背景執行緒依序執行儲存寫入，避免主執行緒做磁碟或資料庫 I/O；失敗交給 onError（錯誤文字鍵、例外）。 */
public final class WriteQueue {
    @FunctionalInterface
    public interface Task { void run() throws IOException; }

    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tokaPortal-writer");
        thread.setDaemon(true);
        return thread;
    });
    private final BiConsumer<String, Throwable> onError;

    public WriteQueue(BiConsumer<String, Throwable> onError) {
        this.onError = onError;
    }

    public void submit(String errorKey, Task task) {
        executor.execute(() -> {
            try { task.run(); }
            catch (Throwable error) { onError.accept(errorKey, error); }
        });
    }

    /** 等已排入的寫入完成；逾時回傳 false。 */
    public boolean drain(long timeoutMillis) {
        try {
            executor.submit(() -> {}).get(timeoutMillis, TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException | ExecutionException | RejectedExecutionException e) {
            return false;
        }
    }

    public void shutdown() {
        executor.shutdown();
    }
}
