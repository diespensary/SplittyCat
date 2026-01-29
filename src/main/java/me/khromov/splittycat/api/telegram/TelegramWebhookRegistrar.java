package me.khromov.splittycat.api.telegram;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.security.config.SecurityProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TelegramWebhookRegistrar {

    private final SecurityProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        String url = props.getTelegram().getWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        var form = new LinkedMultiValueMap<String, String>();
        form.add("url", url);

        String secret = props.getTelegram().getWebhookSecret();
        if (secret != null && !secret.isBlank()) {
            form.add("secret_token", secret);
        }

        RestClient.create("https://api.telegram.org/bot" + props.getTelegram().getBotToken())
                .post()
                .uri("/setWebhook")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
