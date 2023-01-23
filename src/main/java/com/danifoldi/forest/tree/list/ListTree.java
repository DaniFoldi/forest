package com.danifoldi.forest.tree.list;

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

import java.util.List;
import java.util.concurrent.CompletableFuture;

@VersionCollector("1.0.0")
@DependencyCollector(tree="command", minVersion="1.0.0")
public class ListTree implements Tree, CommandContainer {
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            GrownTrees.get(CommandTree.class).registerCommands(this);
        }, Microbase.getThreadPool("list"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            GrownTrees.get(CommandTree.class).unregisterCommands(this);
            return Microbase.shutdownThreadPool("list", 1000, force);
        });
    }

    @CommandDefinition(route="list", permission="forest.list.command.list", runAsync=true)
    @CommandCollector("list")
    @MessageCollector(value="list.playercount", replacements={"{count}"})
    @MessageCollector(value="list.server.{server}", replacements={"{count}"})
    @PermissionCollector("forest.list.command.list")
    public void onList(@Source BaseSender sender) {
        long count = Microbase.getPlatform().getPlayers().stream().filter(sender::canSee).count();
        sender.send(Microbase.baseMessage().providedText("list.playercount").replace("{count}", String.valueOf(count)));
        Microbase.getPlatform().getServers().forEach((id, server) -> {
            List<BasePlayer> visiblePlayers = server.players().stream().filter(sender::canSee).toList();
            BaseMessage message = Microbase.baseMessage().providedText("list.server.%s".formatted(id)).replace("{count}", String.valueOf(visiblePlayers.size()));
            for (BasePlayer player: visiblePlayers) {
                message = message.text().rawText(player.name());
            }
            sender.send(message);
        });
    }
}
