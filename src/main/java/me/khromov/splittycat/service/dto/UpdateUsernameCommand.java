package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

public record UpdateUsernameCommand(
        @NotBlank(message = "Имя пользователя не может быть пустым")
        @Size(max = 255, message = "Имя пользователя не должно быть длиннее 255 символов")
        String username
) {

    public UpdateUsernameCommand {
        username = InputNormalizer.trim(username);
    }
}
