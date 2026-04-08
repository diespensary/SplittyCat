package me.khromov.splittycat.api.mapper;

import me.khromov.splittycat.api.dto.BalanceEntryDto;
import me.khromov.splittycat.api.dto.BalanceResponse;
import me.khromov.splittycat.service.dto.MyBalance;

public final class BalanceApiMapper {

    private BalanceApiMapper() {
    }

    public static BalanceResponse toBalanceResponse(MyBalance balance) {
        return new BalanceResponse(
                balance.myParticipantId(),
                balance.youOwe().stream()
                        .map(entry -> new BalanceEntryDto(entry.participantId(), entry.participantName(), entry.currencyCode(), entry.amount()))
                        .toList(),
                balance.oweYou().stream()
                        .map(entry -> new BalanceEntryDto(entry.participantId(), entry.participantName(), entry.currencyCode(), entry.amount()))
                        .toList()
        );
    }
}
