package com.danifoldi.forest.tree.dataverse;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@VersionCollector("1.0.0")
public class DataverseTree implements Tree {

    @Override
    public @NotNull CompletableFuture<?> load() {
        //noinspection ResultOfMethodCallIgnored
        return CompletableFuture.runAsync(DataVerse::getDataVerse, Microbase.getThreadPool("dataverse"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("dataverse", 1000, force));
    }
}
