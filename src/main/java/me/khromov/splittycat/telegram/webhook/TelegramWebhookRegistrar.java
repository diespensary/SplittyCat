package me.khromov.splittycat.telegram.webhook;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class TelegramWebhookRegistrar {

    private final TelegramProperties props;

    @EventListener(ApplicationReadyEvent.class)
    public void registerWebhook() {
        String url = props.getWebhookUrl();
        if (url == null || url.isBlank()) {
            return;
        }

        LinkedMultiValueMap form = new LinkedMultiValueMap<String, String>();
        form.add("url", url);

        String secret = props.getWebhookSecret();
        if (secret != null && !secret.isBlank()) {
            form.add("secret_token", secret);
        }

        RestClient.create("https://api.telegram.org/bot" + props.getBotToken())
                .post()
                .uri("/setWebhook")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
