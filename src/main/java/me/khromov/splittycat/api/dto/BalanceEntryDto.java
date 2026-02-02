package me.khromov.splittycat.api.dto;

import java.math.BigDecimal;

public record BalanceEntryDto(Long participantId,
                              String participantName,
                              String currencyCode,
                              BigDecimal amount) {
}