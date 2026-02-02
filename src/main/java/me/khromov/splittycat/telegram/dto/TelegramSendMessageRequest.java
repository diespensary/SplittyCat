package me.khromov.splittycat.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramSendMessageRequest(
        @JsonProperty("chat_id") long chatId,
        String text,
        @JsonProperty("reply_markup") TelegramInlineKeyboardMarkup replyMarkup
) {}
