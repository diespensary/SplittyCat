package me.khromov.splittycat.bot.registration;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import me.khromov.splittycat.telegram.dto.TelegramInlineKeyboardButton;
import me.khromov.splittycat.telegram.dto.TelegramInlineKeyboardMarkup;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UsernameChoiceProcessor implements RegistrationStepProcessor {
    private static final String KEEP = "start:keep";
    private static final String CHANGE = "start:change";
    private final UserService userService;
    private final TelegramBotClient botClient;

    @Override
    public RegistrationStep step() {
        return RegistrationStep.USERNAME_CHOICE;
    }

    @Override
    public void sendPrompt(User user, BotMessage message) {
        var markup = new TelegramInlineKeyboardMarkup(List.of(List.of(
                new TelegramInlineKeyboardButton("Оставить", KEEP),
                new TelegramInlineKeyboardButton("Изменить", CHANGE))));
        botClient.sendMessage(message.chatId(),
                "Текущий username: " + user.getUsername() +
                        "\nХотите поменять username или оставляем текущий?",
                markup);
    }

    @Override
    public void handleCallback(User user, BotMessage message, String data) {
        if (KEEP.equals(data)) {
            userService.completeRegistration(user.getTgId());
            botClient.sendMessage(message.chatId(),
                    "Ок ✅ Оставляем: " + user.getUsername() +
                            "\n\nТеперь открой Mini App в меню бота.");
        } else if (CHANGE.equals(data)) {
            userService.proceedToNextStep(user.getTgId());
            botClient.sendMessage(message.chatId(),
                    "Напиши новый username одним сообщением.");
        } else {
            botClient.sendMessage(message.chatId(),
                    "Продолжите регистрацию через /start");
        }
    }

    @Override
    public void handleText(User user, BotMessage message, String text) {
    }
}
