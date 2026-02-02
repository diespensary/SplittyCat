package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.CreateExpenseRequest;
import me.khromov.splittycat.api.dto.ExpenseDetailDto;
import me.khromov.splittycat.api.dto.ExpenseSummaryDto;
import me.khromov.splittycat.api.dto.ParticipantDto;
import me.khromov.splittycat.api.dto.ShareDto;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.service.ExpenseService;
import me.khromov.splittycat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events/{eventId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final ExpenseService expenseService;
    private final ParticipantShareRepository participantShareRepository;

    @GetMapping
    public List<ExpenseSummaryDto> listExpenses(@PathVariable Long eventId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        List<Expense> expenses = expenseService.getExpenses(eventId, user);
        return expenses.stream()
                .map(e -> new ExpenseSummaryDto(
                        e.getId(),
                        e.getTitle(),
                        e.getAmount(),
                        e.getCurrency().getCode(),
                        e.getExpenseDate(),
                        e.getPayerParticipant().getName()))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ExpenseSummaryDto createExpense(@PathVariable Long eventId,
                                           @Valid @RequestBody CreateExpenseRequest request) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Map<Long, BigDecimal> sharesMap = new HashMap<>();
        for (ShareDto share : request.shares()) {
            sharesMap.put(share.participantId(), share.amount());
        }
        Expense expense = expenseService.createExpense(
                eventId,
                request.title(),
                request.amount(),
                request.currencyCode(),
                request.expenseDate(),
                request.payerParticipantId(),
                sharesMap,
                user);
        return new ExpenseSummaryDto(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency().getCode(),
                expense.getExpenseDate(),
                expense.getPayerParticipant().getName());
    }

    @GetMapping("/{expenseId}")
    public ExpenseDetailDto getExpense(@PathVariable Long eventId,
                                       @PathVariable Long expenseId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Expense expense = expenseService.getExpense(eventId, expenseId, user);
        List<ParticipantShare> shares = participantShareRepository.findByExpenseId(expense.getId());
        List<ShareDto> shareDtos = shares.stream()
                .map(s -> new ShareDto(s.getParticipant().getId(), s.getAmount(), s.getDescription()))
                .collect(Collectors.toList());
        Participant payer = expense.getPayerParticipant();
        ParticipantDto payerDto = new ParticipantDto(
                payer.getId(), payer.getName(), payer.getLinkedUser() != null);
        return new ExpenseDetailDto(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency().getCode(),
                expense.getExpenseDate(),
                payerDto,
                shareDtos);
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long eventId,
                              @PathVariable Long expenseId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        expenseService.deleteExpense(eventId, expenseId, user);
    }
}
