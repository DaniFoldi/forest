package com.danifoldi.forest.tree.command;

import com.danifoldi.dataverse.DataVerse;
import com.danifoldi.dataverse.data.NamespacedMultiDataVerse;
import com.danifoldi.forest.seed.GrownTrees;
import com.danifoldi.forest.seed.MessageProvider;
import com.danifoldi.forest.seed.Tree;
import com.danifoldi.forest.seed.collector.collector.DependencyCollector;
import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.forest.seed.collector.collector.VersionCollector;
import com.danifoldi.forest.tree.dataverse.DataverseTree;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import com.google.common.reflect.TypeToken;
import grapefruit.command.CommandContainer;
import grapefruit.command.dispatcher.CommandDispatcher;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

@VersionCollector("1.0.0")
@DependencyCollector(tree="config", minVersion="1.0.0")
@DependencyCollector(tree="message", minVersion="1.0.0")
public class CommandTree implements Tree {

    CommandDispatcher<BaseSender> dispatcher;
    NamespacedMultiDataVerse<CommandConfig> commandDataverse;

    @MessageCollector(value="forest.command.condition.failed", replacements={"{id}"})
    @MessageCollector(value="forest.command.dispatcher.authorization-error", replacements={"{permission}"})
    @MessageCollector(value="forest.command.dispatcher.failed-to-execute-command", replacements={"{commandline}"})
    @MessageCollector(value="forest.command.dispatcher.no-such-command", replacements={"{name}"})
    @MessageCollector(value="forest.command.dispatcher.too-few-arguments", replacements={"{syntax}"})
    @MessageCollector(value="forest.command.dispatcher.too-many-arguments", replacements={"{syntax}"})
    @MessageCollector(value="forest.command.dispatcher.illegal-command-source", replacements={"{found}", "{required}"})
    @MessageCollector(value="forest.command.parameter.invalid-boolean-value", replacements={"{input}", "{options}"})
    @MessageCollector(value="forest.command.parameter.invalid-character-value", replacements={"{input}"})
    @MessageCollector(value="forest.command.parameter.invalid-number-value", replacements={"{input}"})
    @MessageCollector(value="forest.command.parameter.number-out-of-range", replacements={"{min}", "{max}"})
    @MessageCollector(value="forest.command.parameter.quoted-string-invalid-trailing-character", replacements={"{input}"})
    @MessageCollector(value="forest.command.parameter.string-regex-error", replacements={"{input}", "{regex}"})
    @MessageCollector(value="forest.command.parameter.missing-flag-value", replacements={"{input}"})
    @MessageCollector(value="forest.command.parameter.missing-flag", replacements={"{syntax}"})
    @MessageCollector(value="forest.command.parameter.duplicate-flag", replacements={"{flag}"})
    @MessageCollector(value="forest.command.parameter.unrecognized-command-flag", replacements={"{input}"})
    @Override
    public @NotNull CompletableFuture<?> load() {
        return CompletableFuture.runAsync(() -> {
            commandDataverse = DataVerse.getDataVerse().getNamespacedMultiDataVerse(GrownTrees.get(DataverseTree.class), "command_alias", CommandConfig::new);
            dispatcher = CommandDispatcher.builder(TypeToken.of(BaseSender.class))
                    .withAuthorizer(BaseSender::hasPermission)
                    .withMessenger(BaseSender::send)
                    .withMessageProvider(key -> MessageProvider.provide("forest.command.%s".formatted(key.key())))
                    .withAsyncExecutor(Microbase.getThreadPool("command"))
                    .withRegistrationHandler(new CommandRegistrationHandler(commandDataverse))
                    .build();
            dispatcher.mappers().registerMapper(new CommandPlayerMapper());
            dispatcher.mappers().registerMapper(new CommandServerMapper());
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
