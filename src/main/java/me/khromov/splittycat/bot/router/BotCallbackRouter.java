package me.khromov.splittycat.bot.router;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.StartCommandHandler;
import me.khromov.splittycat.bot.dto.BotMessage;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BotCallbackRouter {

    private final StartCommandHandler startCommandHandler;

    public void route(BotMessage message) {
        if (message.callbackData() == null || message.callbackData().isBlank()) {
            return;
        }
        startCommandHandler.onCallback(message);
    }
}
