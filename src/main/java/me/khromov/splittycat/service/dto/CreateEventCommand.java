package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

public record CreateEventCommand(
        @NotBlank(message = "Название не может быть пустым")
        @Size(max = 255, message = "Название не должно быть длиннее 255 символов")
        String title
) {

    public CreateEventCommand {
        title = InputNormalizer.trim(title);
    }
}
