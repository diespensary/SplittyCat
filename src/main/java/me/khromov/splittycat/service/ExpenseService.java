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
import java.util.Objects;

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трата не найдена"));
    }

    @Transactional
    public Expense createExpense(Long eventId, String title, BigDecimal amount,
                                 String currencyCode, LocalDate expenseDate,
                                 Long payerParticipantId, List<ShareInput> shares,
                                 User user) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название не может быть пустым");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Сумма должна быть положительной");
        }
        if (expenseDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите дату");
        }
        if (shares == null || shares.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Доли не могут быть пустыми");
        }
        Event event = requireAccess(eventId, user);
        Currency currency = currencyRepository.findByCodeIgnoreCase(currencyCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Неизвестная валюта"));
        Participant payer = participantRepository.findByIdAndEventId(payerParticipantId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Плательщик не найден в этом событии"));
        BigDecimal sumShares = BigDecimal.ZERO;
        for (ShareInput shareInput : shares) {
            Long participantId = shareInput.participantId();
            BigDecimal shareAmount = shareInput.amount();
            if (shareAmount == null || shareAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Доли должны быть неотрицательными");
            }
            participantRepository.findByIdAndEventId(participantId, event.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Участник не найден в этом событии"));
            sumShares = sumShares.add(shareAmount);
        }
        BigDecimal tolerance = new BigDecimal("0.000000001");

        if (sumShares.subtract(amount).abs().compareTo(tolerance) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Сумма долей должна равняться общей сумме");
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

        for (ShareInput shareInput : shares) {
            ParticipantShare share = new ParticipantShare();
            share.setExpense(expense);
            share.setParticipant(participantRepository.findById(shareInput.participantId()).orElse(null));
            share.setAmount(shareInput.amount());
            share.setDescription(normalizeDescription(shareInput.description()));
            participantShareRepository.save(share);
        }
        return expense;
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ShareInput(Long participantId, BigDecimal amount, String description) {
        public ShareInput {
            Objects.requireNonNull(participantId, "participantId");
            Objects.requireNonNull(amount, "amount");
        }
    }

    @Transactional
    public void deleteExpense(Long eventId, Long expenseId, User user) {
        Event event = requireAccess(eventId, user);
        Expense expense = expenseRepository.findByIdAndEventId(expenseId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Трата не найдена"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean creator = expense.getOwnerUser().getId().equals(user.getId());
        boolean payerIsUser = expense.getPayerParticipant().getLinkedUser() != null &&
                expense.getPayerParticipant().getLinkedUser().getId().equals(user.getId());
        if (!owner && !creator && !payerIsUser) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Удалить трату может только её создатель, владелец события или плательщик");
        }
        expenseRepository.delete(expense);
    }

    private Event requireAccess(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean linked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (!owner && !linked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Не участник этого события");
        }
        return event;
    }
}
