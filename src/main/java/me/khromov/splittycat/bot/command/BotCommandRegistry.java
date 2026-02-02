package me.khromov.splittycat.bot.command;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BotCommandRegistry {
    private final Map<String, BotCommandHandler> handlers = new HashMap<>();

    public BotCommandRegistry(List<BotCommandHandler> handlers) {
        for (BotCommandHandler handler : handlers) {
            this.handlers.put(handler.command(), handler);
        }
    }

    public BotCommandHandler getHandler(String command) {
        return handlers.get(command);
    }
}
