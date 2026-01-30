package me.khromov.splittycat.bot.dto;

public record BotMessage(long tgId, long chatId, String username, String text) {}
