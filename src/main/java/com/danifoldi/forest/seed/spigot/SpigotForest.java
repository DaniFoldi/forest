package com.danifoldi.forest.seed.spigot;

import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@SuppressWarnings("unused")
public class SpigotForest extends JavaPlugin {

    private final TreeLoader loader = new TreeLoader();

    @Override
    public void onEnable() {
        Microbase.setup(Bukkit.getServer(), this, getDataFolder().toPath(), MessageProvider::provide);
        TreeLoader.setInstance(loader);
        loader.fetchMetadata().thenRunAsync(() -> {
            loader.preloadKnownTrees();
            loader.loadTargets().join();
        }, Microbase.getThreadPool("forest")).join();
    }

    @Override
    public void onDisable() {
        loader.unloadTargets(true).join();
    }
}
