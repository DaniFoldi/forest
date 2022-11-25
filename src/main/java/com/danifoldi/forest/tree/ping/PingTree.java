package com.danifoldi.forest.tree.ping;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.tree.command.CommandTree;
import com.danifoldi.microbase.BaseMessage;
import com.danifoldi.microbase.BasePlayer;
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
        return CompletableFuture.runAsync(() -> GrownTrees.get(CommandTree.class).registerCommands(this));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(CommandTree.class).unregisterCommands(this);
            return true;
        });
    }

    @CommandCollector("ping")
    @PermissionCollector("forest.ping.command.ping")
    @CommandDefinition(route = "ping", permission = "forest.ping.command.ping", runAsync = true)
    public void pingCommand(@Source BaseSender sender) {
        BaseMessage message = Microbase.baseMessage().providedText("command.ping.pong");
        if (sender instanceof BasePlayer player) {
            message = message.replace("{ping}", String.valueOf(player.ping()));
        } else {
            message = message.replace("{ping}", Microbase.provideMessage("command.ping.consoleping"));
        }
        sender.send(message);
    }
}
