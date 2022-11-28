package com.danifoldi.forest.tree.command;

import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import grapefruit.command.dispatcher.registration.CommandRegistrationContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class CommandRegistrationHandler implements grapefruit.command.dispatcher.registration.CommandRegistrationHandler<BaseSender> {

    private final NamespacedMultiDataVerse<CommandConfig> commandDataverse;

    public CommandRegistrationHandler(NamespacedMultiDataVerse<CommandConfig> commandDataverse) {
        this.commandDataverse = commandDataverse;
    }

    @Override
    public void accept(final @NotNull CommandRegistrationContext<BaseSender> context) {
        final String[] rootAliases = context.route().get(0).split("\\|");

        Set<String> mappedAliases = Collections.synchronizedSet(new HashSet<>());
        CompletableFuture<?>[] configs = Arrays
                .stream(rootAliases)
                .map(a -> commandDataverse
                        .get(a)
                        .thenAccept(l -> mappedAliases.addAll(l
                                .stream()
                                .map(c -> c.alias)
                                .toList()))).toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(configs).thenAcceptAsync((done) -> {
            Arrays.stream(rootAliases).forEach(Microbase.getPlatform()::unregisterCommand);
            if (context.registration().permission().isPresent()) {
                Microbase.getPlatform().registerCommand(
                        Arrays.asList(rootAliases),
                        context.registration().permission().get(),
                        GrownTrees.get(CommandTree.class).dispatcher::dispatchCommand,
                        GrownTrees.get(CommandTree.class).dispatcher::listSuggestions);
            } else {
                Microbase.getPlatform().registerCommand(
                        Arrays.asList(rootAliases),
                        GrownTrees.get(CommandTree.class).dispatcher::dispatchCommand,
                        GrownTrees.get(CommandTree.class).dispatcher::listSuggestions);
            }
        }, Microbase.getThreadPool("command"));
    }
}
