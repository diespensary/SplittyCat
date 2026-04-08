package me.khromov.splittycat.service.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import me.khromov.splittycat.common.util.InputNormalizer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseCommand(
        @NotBlank(message = "Название не может быть пустым")
        @Size(max = 255, message = "Название не должно быть длиннее 255 символов")
        String title,

        @NotNull(message = "Сумма обязательна")
        @Positive(message = "Сумма должна быть положительной")
        @Digits(integer = 16, fraction = 2, message = "Сумма должна содержать не более 16 цифр до запятой и 2 после")
        BigDecimal amount,

        @NotBlank(message = "Укажите валюту")
        @Pattern(regexp = "[A-Za-z]{3}", message = "Код валюты должен состоять из 3 латинских букв")
        String currencyCode,

        @NotNull(message = "Укажите дату")
        LocalDate expenseDate,

        @NotNull(message = "Укажите плательщика")
        @Positive(message = "payerParticipantId должен быть положительным")
        Long payerParticipantId,

        @NotEmpty(message = "Доли не могут быть пустыми")
        List<@Valid CreateExpenseShareCommand> shares
) {

    public CreateExpenseCommand {
        title = InputNormalizer.trim(title);
        currencyCode = InputNormalizer.uppercase(currencyCode);
        shares = shares == null ? null : List.copyOf(shares);
    }
}
