package com.danifoldi.forest.tree.remote;

import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.CommandCollector;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.PermissionCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.config.ConfigTree;
import com.danifoldi.forest.tree.hazelnut.HazelnutTree;
import com.danifoldi.forest.tree.task.Task;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.BaseServer;
import com.danifoldi.microbase.Microbase;
import com.eclipsesource.json.JsonObject;
import grapefruit.command.CommandDefinition;
import grapefruit.command.dispatcher.Redirect;
import grapefruit.command.parameter.modifier.Source;
import grapefruit.command.parameter.modifier.string.Greedy;
import hazelnut.core.translation.MessageTranslator;
import hazelnut.core.translation.TranslationException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@VersionCollector("1.0.0")
@DependencyCollector(tree="command", minVersion="1.0.0")
@DependencyCollector(tree="console", minVersion="1.0.0")
@DependencyCollector(tree="hazelnut", minVersion="1.0.0")
@DependencyCollector(tree="task", minVersion="1.0.0")
public class RemoteTree implements Tree {

    RemoteConfig config;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            GrownTrees.get(HazelnutTree.class).getHazelnut().translators().add(new MessageTranslator<Task>() {
                @Override
                public @NotNull Class<Task> type() {
                    return Task.class;
                }

                @Override
                public @NotNull JsonObject toIntermediary(@NotNull Task object) throws TranslationException {
                    return new JsonObject()
                            .add("type", object.type)
                            .add("value", object.value);
                }

                @Override
                public @NotNull Task fromIntermediary(@NotNull JsonObject intermediary) throws TranslationException {
                    return new Task(intermediary.getString("type", ""), intermediary.getString("value", ""));
                }
            });
            GrownTrees.get(ConfigTree.class).getConfigFor("remote", true, RemoteConfig::new).thenAcceptAsync(config -> {
                this.config = config;
            }, Microbase.getThreadPool("remote"));
        }, Microbase.getThreadPool("remote"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> Microbase.shutdownThreadPool("remote", 1000, force));
    }

    public void sendTask(RemoteTask task, String target) {
        GrownTrees.get(HazelnutTree.class).getHazelnut().to(target).send(task);
    }

    @CommandCollector("remotecommand")
    @CommandCollector("remote")
    @MessageCollector("remote.invalidKey")
    @PermissionCollector("forest.remote.remote")
    @Redirect(from = "/remotecommand", arguments = {"console"})
    @CommandDefinition(route = "/remote", permission = "forest.remote.remote", runAsync = true)
    public void onRemoteCommand(@Source BaseSender source, String type, BaseServer target, String key, @Greedy String value) {
        if (source.isPlayer()) {
            if (!key.equals(config.key)) {
                source.send(Microbase.baseMessage().providedText("remote.invalidKey"));
                return;
            }
        } else {
            value = "%s %s".formatted(key, value);
        }

        sendTask(new RemoteTask(type, value), target.name());
    }
}
