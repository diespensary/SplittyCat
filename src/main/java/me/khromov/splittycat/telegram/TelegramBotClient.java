package me.khromov.splittycat.telegram;

import me.khromov.splittycat.telegram.dto.TelegramSendMessageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TelegramBotClient {

    private final RestClient client;

    public TelegramBotClient(TelegramProperties props) {
        this.client = RestClient.builder()
                .baseUrl("https://api.telegram.org/bot" + props.getBotToken())
                .build();
    }

    public void sendMessage(long chatId, String text) {
        client.post()
                .uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new TelegramSendMessageRequest(chatId, text))
                .retrieve()
                .toBodilessEntity();
    }
}