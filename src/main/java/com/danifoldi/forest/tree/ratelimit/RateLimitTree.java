package com.danifoldi.forest.tree.ratelimit;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class RateLimitTree implements Tree {

    private static final Map<String, Instant> ratelimits = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(ratelimits::clear, Microbase.getThreadPool("ratelimit"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            ratelimits.clear();
            return Microbase.shutdownThreadPool("ratelimit", 1000, force);
        });
    }

    public static boolean rateLimit(String key, Integer length, ChronoUnit unit) {
        Instant old = ratelimits.getOrDefault(key, Instant.MIN);
        Instant now = Instant.now();
        ratelimits.put(key, now.plus(length, unit));
        return old.isBefore(now);
    }
}
