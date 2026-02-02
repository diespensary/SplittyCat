package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.entity.RegistrationStep;
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
        var user = userService.ensureUser(msg.tgId(), msg.username());

        if (user.isOnboarded()) {
            telegramBotClient.sendMessage(
                    msg.chatId(),
                    "Ваше приложение уже настроено и готово к использованию!"
            );
            return;
        }

        if (user.getRegistrationStep() == RegistrationStep.NONE) {
            userService.startRegistration(msg.tgId());
        }

        var step = userService.getCurrentStep(msg.tgId());
        switch (step) {
            case USERNAME_CHOICE -> {
                var markup = new TelegramInlineKeyboardMarkup(List.of(
                        List.of(
                                new TelegramInlineKeyboardButton("Оставить", CB_KEEP),
                                new TelegramInlineKeyboardButton("Изменить", CB_CHANGE)
                        )
                ));
                telegramBotClient.sendMessage(
                        msg.chatId(),
                        "Текущий username: " + user.getUsername() +
                                "\nХотите поменять username или оставляем текущий?",
                        markup
                );
            }
            case WAITING_USERNAME -> {
                telegramBotClient.sendMessage(
                        msg.chatId(),
                        "Напиши новый username одним сообщением."
                );
            }
            default -> {
                telegramBotClient.sendMessage(msg.chatId(),
                        "Продолжите регистрацию в боте.");
            }
        }
    }

    public void onCallback(BotMessage msg) {
        String data = msg.callbackData();
        if (data == null || data.isBlank()) {
            return;
        }

        var user = userService.requireRegisteredUser(msg.tgId());
        if (user.isOnboarded()) {
            return;
        }

        var step = user.getRegistrationStep();
        if (step == RegistrationStep.USERNAME_CHOICE) {
            if (CB_KEEP.equals(data)) {
                userService.completeRegistration(msg.tgId());
                telegramBotClient.sendMessage(
                        msg.chatId(),
                        "Ок ✅ Оставляем: " + user.getUsername() +
                                "\n\nОткрой Mini App в меню бота."
                );
            }
            if (CB_CHANGE.equals(data)) {
                userService.proceedToNextStep(msg.tgId());
                telegramBotClient.sendMessage(msg.chatId(),
                        "Напиши новый username одним сообщением.");
            }
        }
    }

    public void onText(BotMessage msg) {
        var user = userService.requireRegisteredUser(msg.tgId());
        if (user.isOnboarded()) {
            return;
        }

        if (user.getRegistrationStep() == RegistrationStep.WAITING_USERNAME) {
            String candidate = msg.text() == null ? "" : msg.text().trim();
            if (candidate.isBlank()) {
                telegramBotClient.sendMessage(
                        msg.chatId(),
                        "Username не может быть пустым. Напиши ещё раз."
                );
                return;
            }
            var updated = userService.updateUsernameAndComplete(msg.tgId(), candidate);
            telegramBotClient.sendMessage(
                    msg.chatId(),
                    "Готово ✅ Новый username: " + updated.getUsername() +
                            "\n\nТеперь открой Mini App в меню бота."
            );
        }
    }
}
