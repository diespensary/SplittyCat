package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.UnauthorizedException;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import me.khromov.splittycat.telegram.dto.TelegramUpdatePayload;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bot")
@RequiredArgsConstructor
public class BotWebhookController {

    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final TelegramProperties props;
    private final TelegramUpdateDispatcher dispatcher;

    @PostMapping("/webhook")
    public void onUpdate(
            @RequestHeader(value = SECRET_HEADER, required = false) String secret,
            @RequestBody TelegramUpdatePayload update
    ) {
        String expected = props.getWebhookSecret();
        if (expected != null && !expected.isBlank() && !expected.equals(secret)) {
            throw new UnauthorizedException("Неверный webhook secret");
        }
        dispatcher.dispatch(update);
    }
}
