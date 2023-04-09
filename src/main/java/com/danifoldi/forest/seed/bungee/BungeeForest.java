package com.danifoldi.forest.seed.bungee;

import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

@SuppressWarnings("unused")
public class BungeeForest extends Plugin {

    private final TreeLoader loader = new TreeLoader();
    @Override
    public void onEnable() {
        Microbase.setup(ProxyServer.getInstance(), this, getDataFolder().toPath(), MessageProvider::provide);
        TreeLoader.setInstance(loader);
        loader.fetchMetadata().thenRunAsync(() -> {
            loader.preloadKnownTrees();
            loader.loadTargets().join();
        }, Microbase.getThreadPool("forest")).join();
    }

    @Override
    public void onDisable() {
        loader.unloadTargets(true);
    }
}
