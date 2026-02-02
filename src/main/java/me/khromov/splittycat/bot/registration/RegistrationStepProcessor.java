package me.khromov.splittycat.bot.registration;

import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;

public interface RegistrationStepProcessor {
    RegistrationStep step();
    void sendPrompt(User user, BotMessage message);
    void handleCallback(User user, BotMessage message, String callbackData);
    void handleText(User user, BotMessage message, String text);
}
