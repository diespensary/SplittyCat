package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import me.khromov.splittycat.telegram.dto.TelegramInlineKeyboardButton;
import me.khromov.splittycat.telegram.dto.TelegramInlineKeyboardMarkup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommandHandler {

    private static final String CB_KEEP = "start:keep";
    private static final String CB_CHANGE = "start:change";

    private final UserService userService;
    private final TelegramBotClient telegramBotClient;

    public void onStart(BotMessage msg) {
        userService.ensureUser(msg.tgId(), msg.username());
        userService.clearPendingAction(msg.tgId());

        var user = userService.requireRegisteredUser(msg.tgId());

        var markup = new TelegramInlineKeyboardMarkup(List.of(
                List.of(
                        new TelegramInlineKeyboardButton("Оставить", CB_KEEP),
                        new TelegramInlineKeyboardButton("Изменить", CB_CHANGE)
                )
        ));

        telegramBotClient.sendMessage(
                msg.chatId(),
                "Текущий username: " + user.getUsername() + "\nХотите поменять username или оставляем текущий?",
                markup
        );
    }

    public void onCallback(BotMessage msg) {
        String data = msg.callbackData();
        if (data == null || data.isBlank()) return;

        if (CB_KEEP.equals(data)) {
            userService.clearPendingAction(msg.tgId());
            var user = userService.requireRegisteredUser(msg.tgId());
            telegramBotClient.sendMessage(
                    msg.chatId(),
                    "Ок ✅ Оставляем: " + user.getUsername() + "\n\nОткрой Mini App в меню бота."
            );
            return;
        }

        if (CB_CHANGE.equals(data)) {
            userService.startWaitingUsername(msg.tgId());
            telegramBotClient.sendMessage(msg.chatId(), "Напиши новый username одним сообщением.");
        }
    }

    public void onText(BotMessage msg) {
        if (!userService.isWaitingUsername(msg.tgId())) return;

        String candidate = msg.text() == null ? "" : msg.text().trim();
        if (candidate.isBlank()) {
            telegramBotClient.sendMessage(msg.chatId(), "Username не может быть пустым. Напиши ещё раз.");
            return;
        }

        var user = userService.updateUsername(msg.tgId(), candidate);
        telegramBotClient.sendMessage(
                msg.chatId(),
                "Готово ✅ Новый username: " + user.getUsername() + "\n\nОткрой Mini App в меню бота."
        );
    }
}
