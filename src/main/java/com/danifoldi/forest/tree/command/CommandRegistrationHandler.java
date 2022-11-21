package com.danifoldi.forest.tree.command;

import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.Microbase;
import grapefruit.command.dispatcher.registration.CommandRegistrationContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class CommandRegistrationHandler implements grapefruit.command.dispatcher.registration.CommandRegistrationHandler<BaseSender> {

    @Override
    public void accept(final @NotNull CommandRegistrationContext<BaseSender> context) {
        final String[] rootAliases = context.route().get(0).split("\\|");

        Arrays.stream(rootAliases).forEach(Microbase.getPlatform()::unregisterCommand);
        if (context.registration().permission().isPresent()) {
            Microbase.getPlatform().registerCommand(
                    Arrays.asList(rootAliases),
                    context.registration().permission().get(),
                    CommandTree.dispatcher::dispatchCommand,
                    CommandTree.dispatcher::listSuggestions);
        } else {
            Microbase.getPlatform().registerCommand(
                    Arrays.asList(rootAliases),
                    CommandTree.dispatcher::dispatchCommand,
                    CommandTree.dispatcher::listSuggestions);
        }
    }
}
