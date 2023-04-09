package com.danifoldi.forest.tree.stafflevel;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.microbase.BasePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class StafflevelTree implements Tree {

    @Override
    public @NotNull CompletableFuture<?> load() {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return null;
    }

    public CompletableFuture<Integer> getLevel(BasePlayer player) {
        return GrownTrees.get(ConfigTree.class).getSettingFor("stafflevel", player.uniqueId(), StaffLevel::new).thenApplyAsync(staffLevel -> staffLevel.level);
    }

    public CompletableFuture<?> setLevel(BasePlayer player, int level) {
        return GrownTrees.get(ConfigTree.class).getSettingFor("stafflevel", player.uniqueId(), StaffLevel::new).thenAcceptAsync(staffLevel -> {
            if (staffLevel.visibleLevel > level) {
                return;
            }
            staffLevel.level = level;
            GrownTrees.get(ConfigTree.class).setSettingFor("stafflevel", player.uniqueId(), StaffLevel::new, staffLevel);
        });
    }

    public CompletableFuture<Boolean> canSeeLevel(BasePlayer player, int level) {
        return GrownTrees.get(ConfigTree.class).getSettingFor("stafflevel", player.uniqueId(), StaffLevel::new).thenApplyAsync(staffLevel -> staffLevel.visibleLevel >= level);
    }
}
