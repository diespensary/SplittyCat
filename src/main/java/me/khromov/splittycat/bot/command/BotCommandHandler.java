package me.khromov.splittycat.bot.command;

import me.khromov.splittycat.bot.dto.BotMessage;

public interface BotCommandHandler {
    String command();
    void handle(BotMessage message);
}
