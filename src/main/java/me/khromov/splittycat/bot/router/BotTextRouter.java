package me.khromov.splittycat.bot.router;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.StartCommandHandler;
import me.khromov.splittycat.bot.command.BotCommandHandler;
import me.khromov.splittycat.bot.command.BotCommandRegistry;
import me.khromov.splittycat.bot.dto.BotMessage;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotTextRouter {

    private final BotCommandRegistry commandRegistry;
    private final StartCommandHandler startCommandHandler;

    public void route(BotMessage message) {
        String text = message.text();
        if (text == null || text.isBlank()) {
            return;
        }

        BotCommandHandler handler = resolveCommandHandler(text);
        if (handler != null) {
            handler.handle(message);
            return;
        }

        startCommandHandler.onText(message);
    }

    private BotCommandHandler resolveCommandHandler(String text) {
        if (!text.startsWith("/")) {
            return null;
        }
        String command = text.split("\\s+", 2)[0];
        return commandRegistry.getHandler(command);
    }
}
