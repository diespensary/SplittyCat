package me.khromov.splittycat.service.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

import java.math.BigDecimal;

public record CreateExpenseShareCommand(
        @NotNull(message = "Укажите участника для каждой доли")
        @Positive(message = "participantId должен быть положительным")
        Long participantId,

        @NotNull(message = "Укажите сумму доли")
        @Positive(message = "Сумма доли должна быть положительной")
        @Digits(integer = 16, fraction = 2, message = "Сумма доли должна содержать не более 16 цифр до запятой и 2 после")
        BigDecimal amount,

        @Size(max = 255, message = "Описание доли не должно быть длиннее 255 символов")
        String description
) {

    public CreateExpenseShareCommand {
        description = InputNormalizer.trimToNull(description);
    }
}
