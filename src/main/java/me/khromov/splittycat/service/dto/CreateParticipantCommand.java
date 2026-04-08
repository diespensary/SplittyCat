package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

public record CreateParticipantCommand(
        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 255, message = "Имя не должно быть длиннее 255 символов")
        String name
) {

    public CreateParticipantCommand {
        name = InputNormalizer.trim(name);
    }
}
