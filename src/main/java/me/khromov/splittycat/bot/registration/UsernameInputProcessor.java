package me.khromov.splittycat.bot.registration;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernameInputProcessor implements RegistrationStepProcessor {
    private final UserService userService;
    private final TelegramBotClient botClient;

    @Override
    public RegistrationStep step() {
        return RegistrationStep.WAITING_USERNAME;
    }

    @Override
    public void sendPrompt(User user, BotMessage message) {
        botClient.sendMessage(message.chatId(),
                "Напиши новый username одним сообщением.");
    }

    @Override
    public void handleCallback(User user, BotMessage message, String data) {
        botClient.sendMessage(message.chatId(),
                "Напиши новый username одним сообщением.");
    }

    @Override
    public void handleText(User user, BotMessage message, String text) {
        String candidate = text == null ? "" : text.trim();
        if (candidate.isEmpty()) {
            botClient.sendMessage(message.chatId(),
                    "Username не может быть пустым. Напиши ещё раз.");
            return;
        }
        User updated = userService.updateUsernameAndComplete(user.getTgId(), candidate);
        botClient.sendMessage(message.chatId(),
                "Готово ✅ Новый username: " + updated.getUsername() +
                        "\n\nТеперь открой Mini App в меню бота.");
    }
}
