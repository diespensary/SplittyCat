package me.khromov.splittycat.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.telegram.TelegramProperties;
import me.khromov.splittycat.telegram.dto.TelegramUpdatePayload;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        dispatcher.dispatch(update);
    }
}