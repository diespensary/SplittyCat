package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.command.BotCommandHandler;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.bot.registration.RegistrationProcessorRegistry;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements BotCommandHandler {
    private final UserService userService;
    private final TelegramBotClient botClient;
    private final RegistrationProcessorRegistry registrationRegistry;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(BotMessage message) {
        User user = userService.ensureUser(message.tgId(), message.username());
        if (user.isOnboarded()) {
            botClient.sendMessage(message.chatId(), "Ваше приложение уже настроено и готово к использованию!");
            return;
        }
        userService.startRegistration(message.tgId());
        User updated = userService.requireRegisteredUser(message.tgId());
        registrationRegistry.getProcessor(updated.getRegistrationStep()).sendPrompt(updated, message);
    }

    public void onCallback(BotMessage message) {
        User user = userService.requireRegisteredUser(message.tgId());
        String data = message.callbackData();
        if (data == null || data.isBlank()) {
            return;
        }
        registrationRegistry.getProcessor(user.getRegistrationStep()).handleCallback(user, message, data);
        botClient.answerCallbackQuery(message.callbackQueryId());
    }

    public void onText(BotMessage message) {
        User user = userService.requireRegisteredUser(message.tgId());
        if (user.isOnboarded()) {
            return;
        }
        String text = message.text();
        registrationRegistry.getProcessor(user.getRegistrationStep()).handleText(user, message, text);
    }
}
