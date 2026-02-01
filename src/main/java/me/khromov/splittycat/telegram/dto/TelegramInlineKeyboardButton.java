package me.khromov.splittycat.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramInlineKeyboardButton(
        String text,
        @JsonProperty("callback_data") String callbackData
) {

}
