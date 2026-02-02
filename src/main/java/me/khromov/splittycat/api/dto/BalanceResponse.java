package me.khromov.splittycat.api.dto;

import java.util.List;

public record BalanceResponse(Long myParticipantId,
                              List<BalanceEntryDto> youOwe,
                              List<BalanceEntryDto> oweYou) {
}
