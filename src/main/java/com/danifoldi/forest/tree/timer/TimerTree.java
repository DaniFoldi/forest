package com.danifoldi.forest.tree.timer;

import com.danifoldi.forest.seed.Tree;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class TimerTree implements Tree {

    @Override
    public @NotNull CompletableFuture<?> load() {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return null;
    }
}
