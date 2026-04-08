package me.khromov.splittycat.service;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import me.khromov.splittycat.service.access.EventQueryService;
import me.khromov.splittycat.service.dto.BalanceEntry;
import me.khromov.splittycat.service.dto.MyBalance;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final ParticipantShareRepository shareRepository;
    private final EventQueryService eventQueryService;
    private final EventAccessPolicy eventAccessPolicy;

    public MyBalance getMyBalance(Long eventId, User user) {
        Event event = eventAccessPolicy.requireAccessible(eventQueryService.requireById(eventId), user);
        Participant currentParticipant = eventAccessPolicy.requireLinkedParticipant(event, user);
        List<Expense> expenses = expenseRepository.findByEventId(eventId);

        if (expenses.isEmpty()) {
            return new MyBalance(currentParticipant.getId(), Collections.emptyList(), Collections.emptyList());
        }

        return new BalanceAccumulator(currentParticipant.getId(), loadSharesByExpenseId(expenses))
                .accumulate(expenses)
                .toBalance();
    }

    private Map<Long, List<ParticipantShare>> loadSharesByExpenseId(List<Expense> expenses) {
        List<Long> expenseIds = expenses.stream().map(Expense::getId).toList();
        return shareRepository.findByExpenseIdIn(expenseIds).stream()
                .collect(Collectors.groupingBy(share -> share.getExpense().getId()));
    }

    private record BalanceKey(Long participantId, String currencyCode) {
    }

    private static final class BalanceAccumulator {

        private final Long myParticipantId;
        private final Map<Long, List<ParticipantShare>> sharesByExpenseId;
        private final Map<BalanceKey, BigDecimal> youOwe = new HashMap<>();
        private final Map<BalanceKey, BigDecimal> oweYou = new HashMap<>();
        private final Map<Long, String> participantNames = new HashMap<>();

        private BalanceAccumulator(Long myParticipantId, Map<Long, List<ParticipantShare>> sharesByExpenseId) {
            this.myParticipantId = myParticipantId;
            this.sharesByExpenseId = sharesByExpenseId;
        }

        private BalanceAccumulator accumulate(List<Expense> expenses) {
            expenses.forEach(this::accumulateExpense);
            return this;
        }

        private void accumulateExpense(Expense expense) {
            Participant payer = expense.getPayerParticipant();
            Long payerId = payer.getId();
            String currencyCode = expense.getCurrency().getCode();
            rememberParticipant(payer);

            for (ParticipantShare share : sharesByExpenseId.getOrDefault(expense.getId(), Collections.emptyList())) {
                Participant participant = share.getParticipant();
                Long participantId = participant.getId();
                rememberParticipant(participant);

                if (participantId.equals(myParticipantId) && !payerId.equals(myParticipantId)) {
                    youOwe.merge(new BalanceKey(payerId, currencyCode), share.getAmount(), BigDecimal::add);
                    continue;
                }
                if (payerId.equals(myParticipantId) && !participantId.equals(myParticipantId)) {
                    oweYou.merge(new BalanceKey(participantId, currencyCode), share.getAmount(), BigDecimal::add);
                }
            }
        }

        private void rememberParticipant(Participant participant) {
            participantNames.putIfAbsent(participant.getId(), participant.getName());
        }

        private MyBalance toBalance() {
            return new MyBalance(myParticipantId, toEntries(youOwe), toEntries(oweYou));
        }

        private List<BalanceEntry> toEntries(Map<BalanceKey, BigDecimal> balances) {
            return balances.entrySet().stream()
                    .map(entry -> new BalanceEntry(
                            entry.getKey().participantId(),
                            participantNames.getOrDefault(entry.getKey().participantId(), "Неизвестен"),
                            entry.getKey().currencyCode(),
                            entry.getValue()
                    ))
                    .sorted(Comparator
                            .comparing(BalanceEntry::participantName, Comparator.nullsLast(String::compareToIgnoreCase))
                            .thenComparing(BalanceEntry::currencyCode))
                    .toList();
        }
    }
}
