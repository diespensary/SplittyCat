package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.NotBlank;
import me.khromov.splittycat.common.util.InputNormalizer;

public record JoinEventCommand(
        @NotBlank(message = "Код приглашения не может быть пустым")
        String inviteCode
) {

    public JoinEventCommand {
        inviteCode = InputNormalizer.trim(inviteCode);
    }
}
