package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.bot.command.BotCommandHandler;
import me.khromov.splittycat.bot.dto.BotMessage;
import me.khromov.splittycat.domain.repository.UserRepository;
import me.khromov.splittycat.service.UserAccountService;
import me.khromov.splittycat.telegram.client.TelegramBotClient;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StatsCommandHandler implements BotCommandHandler {

    private final UserAccountService userAccountService;
    private final UserRepository userRepository;
    private final TelegramBotClient botClient;
    private final TelegramProperties telegramProperties;

    @Override
    public String command() {
        return "/stats";
    }

    @Override
    public void handle(BotMessage message) {
        userAccountService.requireOnboardedUser(message.tgId());

        List<Long> adminIds = telegramProperties.getAdminIds();
        boolean isAdmin = adminIds != null && adminIds.contains(message.tgId());
        if (!isAdmin) {
            botClient.sendMessage(message.chatId(), "Недостаточно прав для выполнения этой команды");
            return;
        }

        long count = userRepository.count();
        botClient.sendMessage(message.chatId(), "Всего зарегистрировано пользователей: " + count);
    }
}