package me.khromov.splittycat.service.dto;

import java.math.BigDecimal;

public record BalanceEntry(
        Long participantId,
        String participantName,
        String currencyCode,
        BigDecimal amount
) {
}
