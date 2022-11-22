package com.danifoldi.forest.tree.hazelnut;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.microbase.Microbase;
import hazelnut.core.Hazelnut;
import hazelnut.core.HazelnutBuilder;
import hazelnut.core.Namespace;
import hazelnut.core.config.HazelnutConfigBuilder;
import hazelnut.redis.RedisMessageBusFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class HazelnutTree implements Tree {

    private static Hazelnut hazelnutInstance;

    public static Hazelnut getInstance() {
        return getInstance();
    }

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            HazelnutConfig config = ConfigTree.getConfigFor("hazelnut", true, HazelnutConfig::new).join();
            hazelnutInstance = Hazelnut.forIdentity(TreeLoader.getServerId())
                    .namespace(Namespace.of("hazelnut"))
                    .config(hazelnut.core.config.HazelnutConfig
                            .builder()
                            .cacheExpiryRate(config.cacheExpiryRate, TimeUnit.MILLISECONDS)
                            .cacheHousekeeperRate(config.cacheHouskeeperRate, TimeUnit.MILLISECONDS)
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