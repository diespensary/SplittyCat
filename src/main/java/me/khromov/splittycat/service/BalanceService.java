package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.service.dto.BalanceEntry;
import me.khromov.splittycat.service.dto.MyBalance;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final ParticipantShareRepository shareRepository;

    private record Key(Long participantId, String currencyCode) {}

    @Transactional
    public MyBalance getMyBalance(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        Participant me = participantRepository.findByEventAndLinkedUser(event, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this event"));

        Long myId = me.getId();

        Map<Key, BigDecimal> youOwe = new HashMap<>();
        Map<Key, BigDecimal> oweYou = new HashMap<>();
        Map<Long, String> participantNameById = new HashMap<>();

        List<Expense> expenses = expenseRepository.findByEventId(event.getId());
        for (Expense expense : expenses) {
            Participant payer = expense.getPayerParticipant();
            Long payerId = payer.getId();
            String currency = expense.getCurrency().getCode();

            participantNameById.putIfAbsent(payerId, payer.getName());

            List<ParticipantShare> shares = shareRepository.findByExpenseId(expense.getId());
            for (ParticipantShare share : shares) {
                Participant other = share.getParticipant();
                Long otherId = other.getId();
                BigDecimal amount = share.getAmount();

                participantNameById.putIfAbsent(otherId, other.getName());

                if (otherId.equals(myId) && !payerId.equals(myId)) {
                    youOwe.merge(new Key(payerId, currency), amount, BigDecimal::add);
                } else if (payerId.equals(myId) && !otherId.equals(myId)) {
                    oweYou.merge(new Key(otherId, currency), amount, BigDecimal::add);
                }
            }
        }

        List<BalanceEntry> youOweList = toEntries(youOwe, participantNameById);
        List<BalanceEntry> oweYouList = toEntries(oweYou, participantNameById);

        return new MyBalance(myId, youOweList, oweYouList);
    }

    private static List<BalanceEntry> toEntries(Map<Key, BigDecimal> map, Map<Long, String> nameById) {
        return map.entrySet().stream()
                .map(e -> new BalanceEntry(
                        e.getKey().participantId(),
                        nameById.getOrDefault(e.getKey().participantId(), "Unknown"),
                        e.getKey().currencyCode(),
                        e.getValue()
                ))
                .sorted(Comparator
                        .comparing(BalanceEntry::participantName, Comparator.nullsLast(String::compareToIgnoreCase))
                        .thenComparing(BalanceEntry::currencyCode))
                .collect(Collectors.toList());
    }
}
