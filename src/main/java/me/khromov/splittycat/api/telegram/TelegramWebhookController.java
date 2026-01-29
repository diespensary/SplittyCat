package me.khromov.splittycat.api.telegram;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.security.config.SecurityProperties;
import me.khromov.splittycat.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final SecurityProperties props;
    private final CurrentUserService currentUserService;
    private final TelegramBotClient telegramBotClient;

    @PostMapping("/webhook")
    public void onUpdate(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody Update update
    ) {
        String expected = props.getTelegram().getWebhookSecret();
        if (expected != null && !expected.isBlank()) {
            if (secret == null || !expected.equals(secret)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            }
        }

        if (update.message() == null || update.message().from() == null || update.message().chat() == null) {
            return;
        }

        String text = update.message().text();
        if (text == null || !text.startsWith("/start")) {
            return;
        }

        long tgId = update.message().from().id();
        String username = update.message().from().username();
        long chatId = update.message().chat().id();

        SecurityContextHolder.getContext().setAuthentication(UserAuthentication.botService(tgId));
        try {
            currentUserService.registerOrUpdate(username);
        } finally {
            SecurityContextHolder.clearContext();
        }

        telegramBotClient.sendMessage(chatId, "Готово ✅ Ты зарегистрирован. Теперь открой Mini App в меню бота.");
    }

    public record Update(Message message) {}
    public record Message(String text, Chat chat, From from) {}
    public record Chat(long id) {}
    public record From(long id, String username) {}
}
