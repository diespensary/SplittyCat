package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.*;
import me.khromov.splittycat.domain.repository.CurrencyRepository;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final EventRepository eventRepository;
    private final ExpenseRepository expenseRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantShareRepository participantShareRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional
    public List<Expense> getExpenses(Long eventId, User user) {
        Event event = requireAccess(eventId, user);
        return expenseRepository.findByEventId(event.getId());
    }

    @Transactional
    public Expense getExpense(Long eventId, Long expenseId, User user) {
        Event event = requireAccess(eventId, user);
        return expenseRepository.findByIdAndEventId(expenseId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    @Transactional
    public Expense createExpense(Long eventId, String title, BigDecimal amount,
                                 String currencyCode, LocalDate expenseDate,
                                 Long payerParticipantId, Map<Long, BigDecimal> shares,
                                 User user) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Title cannot be blank");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive");
        }
        if (expenseDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date is required");
        }
        if (shares == null || shares.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shares cannot be empty");
        }
        Event event = requireAccess(eventId, user);
        Currency currency = currencyRepository.findByCodeIgnoreCase(currencyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown currency"));
        Participant payer = participantRepository.findByIdAndEventId(payerParticipantId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payer not found in this event"));
        BigDecimal sumShares = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
            Long participantId = entry.getKey();
            BigDecimal shareAmount = entry.getValue();
            if (shareAmount == null || shareAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Share amounts must be non‑negative");
            }
            participantRepository.findByIdAndEventId(participantId, event.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Participant not found in this event"));
            sumShares = sumShares.add(shareAmount);
        }
        BigDecimal tolerance = new BigDecimal("0.01");

        if (sumShares.subtract(amount).abs().compareTo(tolerance) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sum of shares must equal total amount");
        }
        Expense expense = new Expense();
        expense.setEvent(event);
        expense.setTitle(title.trim());
        expense.setAmount(amount);
        expense.setCurrency(currency);
        expense.setOwnerUser(user);
        expense.setPayerParticipant(payer);
        expense.setExpenseDate(expenseDate);
        expense = expenseRepository.save(expense);

        for (Map.Entry<Long, BigDecimal> entry : shares.entrySet()) {
            ParticipantShare share = new ParticipantShare();
            share.setExpense(expense);
            share.setParticipant(participantRepository.findById(entry.getKey()).orElse(null));
            share.setAmount(entry.getValue());
            participantShareRepository.save(share);
        }
        return expense;
    }

    @Transactional
    public void deleteExpense(Long eventId, Long expenseId, User user) {
        Event event = requireAccess(eventId, user);
        Expense expense = expenseRepository.findByIdAndEventId(expenseId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean creator = expense.getOwnerUser().getId().equals(user.getId());
        boolean payerIsUser = expense.getPayerParticipant().getLinkedUser() != null &&
                expense.getPayerParticipant().getLinkedUser().getId().equals(user.getId());
        if (!owner && !creator && !payerIsUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this expense");
        }
        expenseRepository.delete(expense);
    }

    private Event requireAccess(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean linked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (!owner && !linked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this event");
        }
        return event;
    }
}
