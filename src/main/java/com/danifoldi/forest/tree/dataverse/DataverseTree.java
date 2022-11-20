package com.danifoldi.forest.tree.dataverse;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.forest.seed.Tree;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class DataverseTree implements Tree {

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(DataVerse::getDataVerse);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return null;
    }
}
