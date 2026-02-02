package me.khromov.splittycat.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseSummaryDto(Long id,
                                String title,
                                BigDecimal amount,
                                String currencyCode,
                                LocalDate expenseDate,
                                String payerName) {
}
