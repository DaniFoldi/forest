package com.danifoldi.forest.tree.command;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.tree.dataverse.DataverseNamespace;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import com.google.common.reflect.TypeToken;
import grapefruit.command.CommandContainer;
import grapefruit.command.dispatcher.CommandDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class CommandTree implements Tree {

    CommandDispatcher<BaseSender> dispatcher;
    NamespacedMultiDataVerse<CommandConfig> commandDataverse;

    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            commandDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(DataverseNamespace.get(), "command_alias", CommandConfig::new);
            //noinspection UnstableApiUsage
            dispatcher = CommandDispatcher.builder(TypeToken.of(BaseSender.class))
                    .withAuthorizer(BaseSender::hasPermission)
                    .withMessenger(BaseSender::send)
                    .withMessageProvider(key -> MessageProvider.provide(key.key()))
                    .withAsyncExecutor(Microbase.getThreadPool("command"))
                    .withRegistrationHandler(new CommandRegistrationHandler(commandDataverse))
                    .build();
            dispatcher.mappers().registerMapper(new CommandPlayerMapper());
        }, Microbase.getThreadPool("command"));
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> unload(boolean force) {
        return CompletableFuture.supplyAsync(() -> {
            Microbase.getPlatform().unregisterAllCommands();
            return Microbase.shutdownThreadPool("command", 1000, force);
        });
    }

    public void registerCommands(CommandContainer container) {
        dispatcher.registerCommands(container);
    }

    public void unregisterCommands(CommandContainer container) {
        // TODO implement once grapefruit supports unregistering commands
        Microbase.logger.log(Level.SEVERE, "unregisterCommands is currently not implemented");
    }
}
