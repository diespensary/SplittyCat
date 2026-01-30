package me.khromov.splittycat.telegram.dto;

public record TelegramMessagePayload(
        String text,
        TelegramChatPayload chat,
        TelegramFromPayload from
) {}
