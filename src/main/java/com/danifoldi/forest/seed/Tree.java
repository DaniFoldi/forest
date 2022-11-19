package com.danifoldi.forest.seed;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface Tree {
    @NotNull CompletableFuture<?> load();

    @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force);
}
