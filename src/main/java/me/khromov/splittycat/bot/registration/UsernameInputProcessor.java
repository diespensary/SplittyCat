package me.khromov.splittycat.bot.registration;

import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.UserRegistrationService;
import me.khromov.splittycat.service.dto.UpdateUsernameCommand;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernameInputProcessor implements RegistrationStepProcessor {
    private final UserRegistrationService userRegistrationService;
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
        try {
            User updated = userRegistrationService.updateUsernameAndComplete(user.getTgId(), new UpdateUsernameCommand(text));
            botClient.sendMessage(message.chatId(),
                    "Готово ✅ Новый username: " + updated.getUsername() +
                            "\n\nТеперь открой Mini App в меню бота.");
        } catch (ConstraintViolationException exception) {
            String validationMessage = exception.getConstraintViolations().stream()
                    .map(violation -> violation.getMessage())
                    .filter(messageText -> messageText != null && !messageText.isBlank())
                    .findFirst()
                    .orElse("Проверь введённое значение и попробуй ещё раз.");
            botClient.sendMessage(message.chatId(), validationMessage);
        }
    }
}
