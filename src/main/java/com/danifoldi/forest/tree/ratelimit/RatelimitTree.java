package com.danifoldi.forest.tree.ratelimit;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@VersionCollector("1.0.0")
public class RatelimitTree implements Tree {

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

    public boolean rateLimit(String key, long length, ChronoUnit unit) {
        Instant old = ratelimits.getOrDefault(key, Instant.MIN);
        Instant now = Instant.now();
        boolean allow = old.isBefore(now);
        if (allow) {
            ratelimits.put(key, now.plus(length, unit));
        }
        return allow;
    }
}
