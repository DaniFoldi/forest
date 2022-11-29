package com.danifoldi.forest.tree.logger;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.dataverse.util.Pair;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@VersionCollector("1.0.0")
@DependencyCollector(tree="dataverse", minVersion="1.0.0")
public class LoggerTree implements Tree {

    private LoggerHandler handler;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            List<LoggerFilter> filters = DataVerse.getDataVerse().getNamespacedMultiDataVerse(DataverseNamespace.get(), "logger_filters", LoggerFilter::new).list().join().stream().map(Pair::getSecond).toList();
            NamespacedMultiDataVerse<LogEntry> logDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(DataverseNamespace.get(), "logger", LogEntry::new);
            handler = new LoggerHandler(logDataverse, filters);
            Logger.getGlobal().addHandler(handler);
        }, Microbase.getThreadPool("logger"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            Logger.getGlobal().removeHandler(handler);
            return Microbase.shutdownThreadPool("logger", 1000, force);
        });
    }
}
