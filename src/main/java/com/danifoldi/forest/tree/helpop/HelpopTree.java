package com.danifoldi.forest.tree.helpop;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
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

@VersionCollector("1.0.0")
@DependencyCollector(tree="command", minVersion="1.0.0")
public class HelpopTree implements Tree, CommandContainer {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            GrownTrees.get(CommandTree.class).registerCommands(this);
        }, Microbase.getThreadPool("helpop"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(CommandTree.class).registerCommands(this);
            return Microbase.shutdownThreadPool("helpop", 1000, force);
        });
    }

    @CommandCollector("helpop")
    @PermissionCollector("forest.helpop.command.helpop")
    @MessageCollector(value="command.helpop.sent")
    @MessageCollector(value="command.helpop.none")
    @CommandDefinition(route="helpop", permission="forest.helpop.command.helpop", runAsync=true)
    public void helpopCommand(@Source BaseSender sender) {
        BaseMessage message;
        if (sender instanceof BasePlayer player) {
            message = Microbase.baseMessage().providedText("command.ping.pong").replace("{ping}", String.valueOf(player.ping()));
        } else {
            message = Microbase.baseMessage().providedText("command.ping.consolepong");
        }
        sender.send(message);
    }
}
