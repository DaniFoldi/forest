package com.danifoldi.forest.tree.config;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedDataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseTree;
import com.danifoldi.microbase.Microbase;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@VersionCollector("1.0.0")
@DependencyCollector(tree="message", minVersion="1.0.0")
public class ConfigTree implements Tree {

    private static final Map<String, NamespacedDataVerse<?>> dataverseCache = new ConcurrentHashMap<>();
    private static final Map<String, NamespacedMultiDataVerse<?>> multiDataverseCache = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(dataverseCache::clear);
    }

    public <T> CompletableFuture<T> getConfigFor(String set, boolean global, Supplier<@NotNull T> objectSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (!dataverseCache.containsKey(set)) {
                dataverseCache.put(set, DataVerse.getDataVerse().getNamespacedDataVerse(GrownTrees.get(DataverseTree.class), "config_%s".formatted(set), objectSupplier));
            }
            //noinspection unchecked
            return ((NamespacedDataVerse<T>)dataverseCache.get(set)).get(global ? "__global__" : "__server__%s".formatted(TreeLoader.getServerId())).join();
        }, Microbase.getThreadPool("config"));
    }

    public <T> CompletableFuture<List<T>> getConfigsFor(String set, boolean global, Supplier<@NotNull T> objectSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (!multiDataverseCache.containsKey(set)) {
                multiDataverseCache.put(set, DataVerse.getDataVerse().getNamespacedMultiDataVerse(GrownTrees.get(DataverseTree.class), "config_%s".formatted(set), objectSupplier));
            }
            //noinspection unchecked
            return ((NamespacedMultiDataVerse<T>)multiDataverseCache.get(set)).get(global ? "__global__" : "__server__%s".formatted(TreeLoader.getServerId())).join();
        }, Microbase.getThreadPool("config"));
    }

    public <T> CompletableFuture<?> setConfigFor(String set, boolean global, Supplier<@NotNull T> objectSupplier, T config) {
        return CompletableFuture.runAsync(() -> {
            if (!dataverseCache.containsKey(set)) {
                dataverseCache.put(set, DataVerse.getDataVerse().getNamespacedDataVerse(GrownTrees.get(DataverseTree.class), "config_%s".formatted(set), objectSupplier));
            }
            //noinspection unchecked
            ((NamespacedDataVerse<T>)dataverseCache.get(set)).createOrUpdate(global ? "__global__" : "__server__%s".formatted(TreeLoader.getServerId()), config).join();
        }, Microbase.getThreadPool("config"));
    }

    public <T> CompletableFuture<T> getSettingFor(String set, UUID uuid, Supplier<@NotNull T> objectSupplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (!dataverseCache.containsKey(set)) {
                dataverseCache.put(set, DataVerse.getDataVerse().getNamespacedDataVerse(GrownTrees.get(DataverseTree.class), "setting_%s".formatted(set), objectSupplier));
            }
            //noinspection unchecked
            return ((NamespacedDataVerse<T>)dataverseCache.get(set)).get(uuid).join();
        }, Microbase.getThreadPool("config"));
    }

    public <T> CompletableFuture<?> setSettingFor(String set, UUID uuid, Supplier<@NotNull T> objectSupplier, T config) {
        return CompletableFuture.runAsync(() -> {
            if (!dataverseCache.containsKey(set)) {
                dataverseCache.put(set, DataVerse.getDataVerse().getNamespacedDataVerse(GrownTrees.get(DataverseTree.class), "setting_%s".formatted(set), objectSupplier));
            }
            //noinspection unchecked
            ((NamespacedDataVerse<T>)dataverseCache.get(set)).createOrUpdate(uuid, config).join();
        }, Microbase.getThreadPool("config"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("config", 1000, force));
    }
}
