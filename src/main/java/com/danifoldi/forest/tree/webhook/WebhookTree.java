package com.danifoldi.forest.tree.webhook;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@VersionCollector("1.0.0")
public class WebhookTree implements Tree {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {

        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return null;
    }
}
