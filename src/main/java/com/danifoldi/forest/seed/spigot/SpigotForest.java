package com.danifoldi.forest.seed.spigot;

import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.TreeLoader;
import com.danifoldi.microbase.Microbase;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class SpigotForest extends JavaPlugin {

    private TreeLoader loader = new TreeLoader();

    @Override
    public void onEnable() {
        Microbase.setup(Bukkit.getServer(), this, getDataFolder().toPath(), Executors.newCachedThreadPool(), MessageProvider::provide);
        loader.fetchTargets();
        loader.preloadKnownTrees();
        loader.loadTargets();
    }

    @Override
    public void onDisable() {
        loader.unloadTargets(true);
    }
}
