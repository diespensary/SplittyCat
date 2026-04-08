package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.command.BotCommandHandler;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.bot.registration.RegistrationProcessorRegistry;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.UserAccountService;
import me.khromov.splittycat.service.UserRegistrationService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements BotCommandHandler {
    private final UserAccountService userAccountService;
    private final UserRegistrationService userRegistrationService;
    private final TelegramBotClient botClient;
    private final RegistrationProcessorRegistry registrationRegistry;

    @Override
    public String command() {
        return "/start";
    }

    @Override
    public void handle(BotMessage message) {
        User user = userAccountService.ensureUser(message.tgId(), message.username());
        if (user.isOnboarded()) {
            botClient.sendMessage(message.chatId(), "Ваше приложение уже настроено и готово к использованию!");
            return;
        }
        userRegistrationService.startRegistration(message.tgId());
        User updated = userAccountService.requireRegisteredUser(message.tgId());
        registrationRegistry.getProcessor(updated.getRegistrationStep()).sendPrompt(updated, message);
    }

    public void onCallback(BotMessage message) {
        User user = userAccountService.requireRegisteredUser(message.tgId());
        String data = message.callbackData();
        if (data == null || data.isBlank()) {
            return;
        }
        registrationRegistry.getProcessor(user.getRegistrationStep()).handleCallback(user, message, data);
        botClient.answerCallbackQuery(message.callbackQueryId());
    }

    public void onText(BotMessage message) {
        User user = userAccountService.requireRegisteredUser(message.tgId());
        if (user.isOnboarded()) {
            return;
        }
        String text = message.text();
        registrationRegistry.getProcessor(user.getRegistrationStep()).handleText(user, message, text);
    }
}
