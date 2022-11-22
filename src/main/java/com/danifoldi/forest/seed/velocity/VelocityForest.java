package com.danifoldi.forest.seed.velocity;

import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.nio.file.Path;

@Plugin(id = "forest",
        name = "Forest",
        version = "@version@",
        description = "Forest is a module system of Trees (microservices)",
        authors = {"DaniFoldi"})
public class VelocityForest {

    private final Path datafolder;
    private final ProxyServer proxyServer;

    private final TreeLoader loader = new TreeLoader();

    @Inject
    public VelocityForest(@DataDirectory Path datafolder, ProxyServer proxyServer) {
        this.datafolder = datafolder;
        this.proxyServer = proxyServer;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        Microbase.setup(proxyServer, this, datafolder, MessageProvider::provide);
        loader.fetchMetadata();
        loader.preloadKnownTrees();
        loader.loadTargets();
    }

    @Subscribe
    public void onShutdown(ProxyShutdownEvent event) {
        loader.unloadTargets(true);
    }
}
