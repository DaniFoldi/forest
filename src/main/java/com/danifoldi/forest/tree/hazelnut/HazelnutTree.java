package com.danifoldi.forest.tree.hazelnut;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.microbase.Microbase;
import hazelnut.core.Hazelnut;
import hazelnut.core.Namespace;
import hazelnut.redis.RedisMessageBusFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@VersionCollector("1.0.0")
public class HazelnutTree implements Tree {

    private Hazelnut hazelnutInstance;

    public Hazelnut getHazelnut() {
        return hazelnutInstance;
    }

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            HazelnutConfig config = GrownTrees.get(ConfigTree.class).getConfigFor("hazelnut", true, HazelnutConfig::new).join();
            hazelnutInstance = Hazelnut.forIdentity(TreeLoader.getServerId())
                    .namespace(Namespace.of("hazelnut"))
                    .config(hazelnut.core.config.HazelnutConfig
                            .builder()
                            .cacheExpiryRate(config.cacheExpiryRate, TimeUnit.MILLISECONDS)
                            .cacheHousekeeperRate(config.cacheHousekeeperRate, TimeUnit.MILLISECONDS)
                            .heartbeatRate(config.heartbeatRate, TimeUnit.MILLISECONDS)
                            .build())
                    .busFactory(RedisMessageBusFactory
                            .builder()
                            .host(config.redisHost)
                            .username(config.redisUsername)
                            .password(config.redisPassword)
                            .build())
                    .build();
        }, Microbase.getThreadPool("hazelnut"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                hazelnutInstance.close();
                return Microbase.shutdownThreadPool("hazelnut", 1000, force);
            } catch (Exception e) {
                return false;
            }
        });
    }
}
