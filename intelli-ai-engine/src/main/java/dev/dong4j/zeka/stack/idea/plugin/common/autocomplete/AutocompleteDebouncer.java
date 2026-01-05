package dev.dong4j.zeka.stack.idea.plugin.common.autocomplete;

import com.intellij.util.concurrency.AppExecutorUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class AutocompleteDebouncer {
    private final ScheduledExecutorService scheduler = AppExecutorUtil.getAppScheduledExecutorService();
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    void debounce(@NotNull String key, long delayMs, @NotNull Runnable task) {
        ScheduledFuture<?> previous = tasks.remove(key);
        if (previous != null) {
            previous.cancel(false);
        }
        ScheduledFuture<?> future = scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        tasks.put(key, future);
    }

    void cancel(@NotNull String key) {
        ScheduledFuture<?> previous = tasks.remove(key);
        if (previous != null) {
            previous.cancel(false);
        }
    }
}
