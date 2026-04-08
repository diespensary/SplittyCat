package me.khromov.splittycat.bot.command;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BotCommandRegistry {
    private final Map<String, BotCommandHandler> handlers;

    public BotCommandRegistry(List<BotCommandHandler> handlers) {
        this.handlers = handlers.stream()
                .collect(Collectors.toUnmodifiableMap(BotCommandHandler::command, Function.identity()));
    }

    public BotCommandHandler getHandler(String command) {
        return handlers.get(command);
    }
}
