package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.*;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;
    private final ParticipantShareRepository shareRepository;

    public static class BalanceEntry {
        public final Long participantId;
        public final String participantName;
        public final String currencyCode;
        public final BigDecimal amount;
        public BalanceEntry(Long participantId, String participantName, String currencyCode, BigDecimal amount) {
            this.participantId = participantId;
            this.participantName = participantName;
            this.currencyCode = currencyCode;
            this.amount = amount;
        }
    }
    public static class MyBalance {
        public final Long myParticipantId;
        public final List<BalanceEntry> youOwe;
        public final List<BalanceEntry> oweYou;
        public MyBalance(Long myParticipantId, List<BalanceEntry> youOwe, List<BalanceEntry> oweYou) {
            this.myParticipantId = myParticipantId;
            this.youOwe = youOwe;
            this.oweYou = oweYou;
        }
    }

    @Transactional
    public MyBalance getMyBalance(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Participant me = participantRepository.findByEventAndLinkedUser(event, user)
                .orElse(null);
        if (me == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this event");
        }
        Long myId = me.getId();
        Map<String, BigDecimal> oweMap = new HashMap<>();
        Map<String, BigDecimal> owedMap = new HashMap<>();
        Map<String, Participant> participantLookup = new HashMap<>();
        for (Expense expense : expenseRepository.findByEventId(event.getId())) {
            Participant payer = expense.getPayerParticipant();
            String currency = expense.getCurrency().getCode();
            List<ParticipantShare> shares = shareRepository.findByExpenseId(expense.getId());
            for (ParticipantShare share : shares) {
                Participant other = share.getParticipant();
                BigDecimal amount = share.getAmount();
                if (other.getId().equals(myId) && !payer.getId().equals(myId)) {
                    String key = payer.getId() + ":" + currency;
                    oweMap.merge(key, amount, BigDecimal::add);
                    participantLookup.putIfAbsent(key, payer);
                } else if (payer.getId().equals(myId) && !other.getId().equals(myId)) {
                    String key = other.getId() + ":" + currency;
                    owedMap.merge(key, amount, BigDecimal::add);
                    participantLookup.putIfAbsent(key, other);
                }
            }
        }
        List<BalanceEntry> youOwe = new ArrayList<>();
        for (var entry : oweMap.entrySet()) {
            String key = entry.getKey();
            BigDecimal amount = entry.getValue();
            Participant p = participantLookup.get(key);
            String currency = key.split(":")[1];
            youOwe.add(new BalanceEntry(p.getId(), p.getName(), currency, amount));
        }
        List<BalanceEntry> oweYou = new ArrayList<>();
        for (var entry : owedMap.entrySet()) {
            String key = entry.getKey();
            BigDecimal amount = entry.getValue();
            Participant p = participantLookup.get(key);
            String currency = key.split(":")[1];
            oweYou.add(new BalanceEntry(p.getId(), p.getName(), currency, amount));
        }
        return new MyBalance(myId, youOwe, oweYou);
    }
}
