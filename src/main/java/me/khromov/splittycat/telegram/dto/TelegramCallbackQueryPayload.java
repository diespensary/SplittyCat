package me.khromov.splittycat.telegram.dto;

public record TelegramCallbackQueryPayload(
        String id,
        TelegramFromPayload from,
        TelegramMessagePayload message,
        String data
) {}
