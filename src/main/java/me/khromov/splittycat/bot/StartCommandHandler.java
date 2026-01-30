package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler {

    private final UserService userService;
    private final TelegramBotClient telegramBotClient;

    public void handle(BotMessage message) {
        userService.registerOrUpdate(message.tgId(), message.username());
        telegramBotClient.sendMessage(
                message.chatId(),
                "Готово ✅ Ты зарегистрирован. Теперь открой Mini App в меню бота."
        );
    }
}
