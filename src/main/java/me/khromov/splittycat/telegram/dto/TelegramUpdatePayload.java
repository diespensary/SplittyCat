package me.khromov.splittycat.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramUpdatePayload(
        TelegramMessagePayload message,
        @JsonProperty("callback_query") TelegramCallbackQueryPayload callbackQuery
) {}
