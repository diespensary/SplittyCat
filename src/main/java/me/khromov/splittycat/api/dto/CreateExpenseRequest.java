package me.khromov.splittycat.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateExpenseRequest(
        @NotBlank String title,
        @NotNull BigDecimal amount,
        @NotBlank String currencyCode,
        @NotNull LocalDate expenseDate,
        @NotNull Long payerParticipantId,
        @NotEmpty List<ShareDto> shares
) {
}
