package me.khromov.splittycat.service.dto;

import java.util.List;

public record MyBalance(
        Long myParticipantId,
        List<BalanceEntry> youOwe,
        List<BalanceEntry> oweYou
) {
}
