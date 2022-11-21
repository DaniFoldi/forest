package com.danifoldi.forest.tree.ping;

import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.tree.command.CommandTree;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import grapefruit.command.CommandContainer;
import grapefruit.command.CommandDefinition;
import grapefruit.command.parameter.modifier.Source;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class PingTree implements Tree, CommandContainer {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            CommandTree.registerCommands(this);
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            CommandTree.unregisterCommands(this);
            return true;
        });
    }

    @CommandDefinition(route = "ping", permission = "forest.ping.command.ping", runAsync = true)
    public void pingCommand(@Source BaseSender sender) {
        sender.send(Microbase.baseMessage().providedText("command.ping.pong"));
    }
}
