package me.khromov.splittycat.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseDetailDto(Long id,
                               String title,
                               BigDecimal amount,
                               String currencyCode,
                               LocalDate expenseDate,
                               ParticipantDto payer,
                               List<ShareDto> shares) {
}
