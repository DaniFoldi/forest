package com.danifoldi.forest.tree.command;

import com.danifoldi.forest.seed.collector.collector.MessageCollector;
import com.danifoldi.microbase.BaseSender;
import com.danifoldi.microbase.BaseServer;
import com.danifoldi.microbase.Microbase;
import com.google.common.reflect.TypeToken;
import grapefruit.command.dispatcher.CommandContext;
import grapefruit.command.dispatcher.CommandInput;
import grapefruit.command.message.Message;
import grapefruit.command.message.MessageKey;
import grapefruit.command.message.Template;
import grapefruit.command.parameter.mapper.ParameterMapper;
import grapefruit.command.parameter.mapper.ParameterMappingException;
import grapefruit.command.util.AnnotationList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class CommandServerMapper implements ParameterMapper<BaseSender, BaseServer>  {
    @SuppressWarnings("UnstableApiUsage")
    @Override
    public @NotNull TypeToken<BaseServer> type() {
        return TypeToken.of(BaseServer.class);
    }

    @Override
    @MessageCollector(value = "command.serverNotFound", replacements = {"{server}"})
    public @NotNull BaseServer map(@NotNull CommandContext<BaseSender> context, @NotNull Queue<CommandInput> args, @NotNull AnnotationList modifiers) throws ParameterMappingException {
        String input = args.element().rawArg();
        return Optional.ofNullable(Microbase.getPlatform().getServers().get(input)).orElseThrow(
                () -> new ParameterMappingException(Message.of(MessageKey.of("command.serverNotFound"), Template.of("{server}", input))));
    }

    @Override
    public @NotNull List<String> listSuggestions(@NotNull CommandContext<BaseSender> context, @NotNull String currentArg, @NotNull AnnotationList modifiers) {
        return Microbase.getPlatform().getServers().keySet().stream().toList();
    }
}
