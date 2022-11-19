package com.danifoldi.forest.seed.bungee;

import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;

import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class BungeeForest extends Plugin {

    private TreeLoader loader = new TreeLoader();
    @Override
    public void onEnable() {
        Microbase.setup(ProxyServer.getInstance(), this, getDataFolder().toPath(), Executors.newCachedThreadPool(), MessageProvider::provide);
        loader.fetchTargets();
        loader.preloadKnownTrees();
        loader.loadTargets();
    }

    @Override
    public void onDisable() {
        loader.unloadTargets(true);
    }
}
