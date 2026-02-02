package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.telegram.dto.TelegramCallbackQueryPayload;
import me.khromov.splittycat.telegram.dto.TelegramMessagePayload;
import me.khromov.splittycat.telegram.dto.TelegramUpdatePayload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {

    private final StartCommandHandler startCommandHandler;

    public void dispatch(TelegramUpdatePayload update) {
        BotMessage msg = normalize(update);
        if (msg == null) return;

        if (msg.callbackData() != null && !msg.callbackData().isBlank()) {
            startCommandHandler.onCallback(msg);
            return;
        }

        String text = msg.text();
        if (text == null || text.isBlank()) return;

        if (text.startsWith("/start")) {
            startCommandHandler.onStart(msg);
            return;
        }

        startCommandHandler.onText(msg);
    }

    private static BotMessage normalize(TelegramUpdatePayload update) {
        if (update == null) return null;

        TelegramMessagePayload m = update.message();
        if (m != null && m.from() != null && m.chat() != null) {
            return new BotMessage(
                    m.from().id(),
                    m.chat().id(),
                    m.from().username(),
                    m.text(),
                    null,
                    null
            );
        }

        TelegramCallbackQueryPayload cq = update.callbackQuery();
        if (cq != null && cq.from() != null && cq.message() != null && cq.message().chat() != null) {
            var cm = cq.message();
            return new BotMessage(
                    cq.from().id(),
                    cm.chat().id(),
                    cq.from().username(),
                    cm.text(),
                    cq.data(),
                    cq.id()
            );
        }

        return null;
    }
}
