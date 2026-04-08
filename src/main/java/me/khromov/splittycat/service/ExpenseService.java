package me.khromov.splittycat.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.NotFoundException;
import me.khromov.splittycat.common.exception.UnprocessableEntityException;
import me.khromov.splittycat.domain.entity.Currency;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.CurrencyRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import me.khromov.splittycat.service.access.EventQueryService;
import me.khromov.splittycat.service.dto.CreateExpenseCommand;
import me.khromov.splittycat.service.dto.CreateExpenseShareCommand;
import me.khromov.splittycat.service.dto.ExpenseDetails;
import me.khromov.splittycat.service.policy.ExpenseDeletionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Validated
@RequiredArgsConstructor
public class ExpenseService {

    private static final BigDecimal SHARE_SUM_TOLERANCE = new BigDecimal("0.000000001");
    private static final String PARTICIPANT_NOT_FOUND_MESSAGE = "Участник не найден в этом событии";
    private static final String PAYER_NOT_FOUND_MESSAGE = "Плательщик не найден в этом событии";

    private final ExpenseRepository expenseRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantShareRepository participantShareRepository;
    private final CurrencyRepository currencyRepository;
    private final EventQueryService eventQueryService;
    private final EventAccessPolicy eventAccessPolicy;
    private final ExpenseDeletionPolicy expenseDeletionPolicy;

    public List<Expense> getExpenses(Long eventId, User user) {
        return expenseRepository.findByEventId(requireAccessibleEvent(eventId, user).getId());
    }

    public Expense getExpense(Long eventId, Long expenseId, User user) {
        return expenseRepository.findByIdAndEventId(expenseId, requireAccessibleEvent(eventId, user).getId())
                .orElseThrow(() -> new NotFoundException("Трата не найдена"));
    }

    public ExpenseDetails getExpenseDetails(Long eventId, Long expenseId, User user) {
        Expense expense = getExpense(eventId, expenseId, user);
        return new ExpenseDetails(expense, participantShareRepository.findByExpenseId(expense.getId()));
    }

    @Transactional
    public Expense createExpense(Long eventId, @Valid CreateExpenseCommand command, User user) {
        Event event = requireAccessibleEvent(eventId, user);
        ExpenseCreationContext context = resolveCreationContext(event, command);
        validateShareSum(command.shares(), command.amount());

        Expense expense = expenseRepository.save(Expense.create(
                event,
                command.title(),
                command.amount(),
                context.currency(),
                user,
                context.payer(),
                command.expenseDate()
        ));
        participantShareRepository.saveAll(buildShares(expense, command.shares(), context.participantsById()));
        return expense;
    }

    @Transactional
    public void deleteExpense(Long eventId, Long expenseId, User user) {
        Event event = requireAccessibleEvent(eventId, user);
        Expense expense = expenseRepository.findByIdAndEventId(expenseId, event.getId())
                .orElseThrow(() -> new NotFoundException("Трата не найдена"));

        expenseDeletionPolicy.validate(event, expense, user);
        expenseRepository.delete(expense);
    }

    private Event requireAccessibleEvent(Long eventId, User user) {
        return eventAccessPolicy.requireAccessible(eventQueryService.requireById(eventId), user);
    }

    private ExpenseCreationContext resolveCreationContext(Event event, CreateExpenseCommand command) {
        Currency currency = requireCurrency(command.currencyCode());
        Map<Long, Participant> participantsById = loadParticipants(event.getId(), command);
        Participant payer = requireParticipant(participantsById, command.payerParticipantId(), PAYER_NOT_FOUND_MESSAGE);
        ensureParticipantsExist(command.shares(), participantsById);
        return new ExpenseCreationContext(currency, payer, participantsById);
    }

    private Currency requireCurrency(String currencyCode) {
        return currencyRepository.findByCodeIgnoreCase(currencyCode)
                .orElseThrow(() -> new UnprocessableEntityException("Неизвестная валюта"));
    }

    private Map<Long, Participant> loadParticipants(Long eventId, CreateExpenseCommand command) {
        Set<Long> participantIds = Stream.concat(
                        Stream.of(command.payerParticipantId()),
                        command.shares().stream().map(CreateExpenseShareCommand::participantId))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return participantRepository.findByEventIdAndIdIn(eventId, participantIds).stream()
                .collect(Collectors.toMap(Participant::getId, Function.identity()));
    }

    private void ensureParticipantsExist(List<CreateExpenseShareCommand> shares, Map<Long, Participant> participantsById) {
        for (CreateExpenseShareCommand share : shares) {
            requireParticipant(participantsById, share.participantId(), PARTICIPANT_NOT_FOUND_MESSAGE);
        }
    }

    private void validateShareSum(List<CreateExpenseShareCommand> shares, BigDecimal totalAmount) {
        BigDecimal sharesSum = shares.stream()
                .map(CreateExpenseShareCommand::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sharesSum.subtract(totalAmount).abs().compareTo(SHARE_SUM_TOLERANCE) > 0) {
            throw new UnprocessableEntityException("Сумма долей должна равняться общей сумме");
        }
    }

    private List<ParticipantShare> buildShares(Expense expense,
                                               List<CreateExpenseShareCommand> shares,
                                               Map<Long, Participant> participantsById) {
        return shares.stream()
                .map(share -> ParticipantShare.create(
                        expense,
                        requireParticipant(participantsById, share.participantId(), PARTICIPANT_NOT_FOUND_MESSAGE),
                        share.amount(),
                        share.description()
                ))
                .toList();
    }

    private Participant requireParticipant(Map<Long, Participant> participantsById,
                                           Long participantId,
                                           String message) {
        Participant participant = participantsById.get(participantId);
        if (participant == null) {
            throw new UnprocessableEntityException(message);
        }
        return participant;
    }

    private record ExpenseCreationContext(Currency currency,
                                          Participant payer,
                                          Map<Long, Participant> participantsById) {
    }
}
