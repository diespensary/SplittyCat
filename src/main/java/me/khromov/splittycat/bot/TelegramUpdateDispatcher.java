package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.bot.router.BotCallbackRouter;
import me.khromov.splittycat.bot.router.BotTextRouter;
import me.khromov.splittycat.telegram.dto.TelegramCallbackQueryPayload;
import me.khromov.splittycat.telegram.dto.TelegramMessagePayload;
import me.khromov.splittycat.telegram.dto.TelegramUpdatePayload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramUpdateDispatcher {

    private final BotTextRouter textRouter;
    private final BotCallbackRouter callbackRouter;

    public void dispatch(TelegramUpdatePayload update) {
        BotMessage message = normalize(update);
        if (message == null) {
            return;
        }
        if (message.callbackData() != null) {
            callbackRouter.route(message);
            return;
        }
        textRouter.route(message);
    }

    private static BotMessage normalize(TelegramUpdatePayload update) {
        if (update == null) {
            return null;
        }

        TelegramMessagePayload message = update.message();
        if (message != null && message.from() != null && message.chat() != null) {
            return new BotMessage(
                    message.from().id(),
                    message.chat().id(),
                    message.from().username(),
                    message.text(),
                    null,
                    null
            );
        }

        TelegramCallbackQueryPayload callbackQuery = update.callbackQuery();
        if (callbackQuery != null && callbackQuery.from() != null
                && callbackQuery.message() != null && callbackQuery.message().chat() != null) {
            TelegramMessagePayload callbackMessage = callbackQuery.message();
            return new BotMessage(
                    callbackQuery.from().id(),
                    callbackMessage.chat().id(),
                    callbackQuery.from().username(),
                    callbackMessage.text(),
                    callbackQuery.data(),
                    callbackQuery.id()
            );
        }

        return null;
    }
}
