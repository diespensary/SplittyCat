package me.khromov.splittycat.api.telegram;

import me.khromov.splittycat.security.config.SecurityProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TelegramBotClient {

    private final RestClient client;

    public TelegramBotClient(SecurityProperties props) {
        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + props.getTelegram().getBotToken())
                .build();
    }

    public void sendMessage(long chatId, String text) {
        client.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new SendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }

    public record SendMessageRequest(long chat_id, String text) {}
}
