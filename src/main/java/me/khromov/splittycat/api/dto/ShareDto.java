package me.khromov.splittycat.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ShareDto(@NotNull Long participantId,
                       @NotNull BigDecimal amount,
                       String description) {
}
