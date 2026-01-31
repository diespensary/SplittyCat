package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler {

    private final UserService userService;
    private final TelegramBotClient telegramBotClient;

    public void handle(BotMessage message) {
        boolean alreadyRegistered = userService.isRegistered(message.tgId());
        userService.registerOrUpdate(message.tgId(), message.username());
        if (alreadyRegistered) {
            telegramBotClient.sendMessage(
                    message.chatId(),
                    "Ваше приложение уже настроено и готово к использованию!\n" +
                            "\nПросто откройте Mini App!"
            );
        } else {
            telegramBotClient.sendMessage(
                    message.chatId(),
                    "Поздравляю! Ваше приложение настроено.\n" +
                            "\nПросто откройте Mini App!"
            );
        }
    }
}

