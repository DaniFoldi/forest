package com.danifoldi.forest.tree.task;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.forest.seed.Tree;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class TaskTree implements Tree {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {

        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return null;
    }

    public static void run(Task task) {

    }
}
