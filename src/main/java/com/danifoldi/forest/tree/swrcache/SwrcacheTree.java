package com.danifoldi.forest.tree.swrcache;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.tree.ratelimit.RateLimitTree;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class SwrcacheTree implements Tree {

    private static final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {

        });
    }

    public <T> T getCached(String namespace, String key, Supplier<T> refresher, long staleAfterMs) {
        String cacheKey = "%s_%s".formatted(namespace, key);
        if (GrownTrees.get(RateLimitTree.class).rateLimit(cacheKey, staleAfterMs, ChronoUnit.MILLIS)) {
            CompletableFuture.runAsync(() -> {
                cache.put(cacheKey, refresher.get());
            }, Microbase.getThreadPool("swrcache"));
        }
        //noinspection unchecked
        return (T)cache.get("%s_%s".formatted(namespace, key));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("swrcache", 1000, force));
    }
}
