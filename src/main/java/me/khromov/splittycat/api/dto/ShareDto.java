package me.khromov.splittycat.api.dto;

import java.math.BigDecimal;

public record ShareDto(Long participantId,
                       BigDecimal amount,
                       String description) {
}
