package me.khromov.splittycat.telegram.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TelegramInlineKeyboardMarkup(
        @JsonProperty("inline_keyboard") List<List<TelegramInlineKeyboardButton>> inlineKeyboard
) {}
