package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.ExpenseDetailDto;
import me.khromov.splittycat.api.dto.ExpenseSummaryDto;
import me.khromov.splittycat.api.mapper.ExpenseApiMapper;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.ExpenseService;
import me.khromov.splittycat.service.dto.CreateExpenseCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ApiUserProvider apiUserProvider;
    private final ExpenseService expenseService;

    @GetMapping
    public List<ExpenseSummaryDto> listExpenses(@PathVariable Long eventId) {
        return ExpenseApiMapper.toExpenseSummaryDtos(expenseService.getExpenses(eventId, currentUser()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseSummaryDto createExpense(@PathVariable Long eventId,
                                           @Valid @RequestBody CreateExpenseCommand command) {
        return ExpenseApiMapper.toExpenseSummaryDto(expenseService.createExpense(eventId, command, currentUser()));
    }

    @GetMapping("/{expenseId}")
    public ExpenseDetailDto getExpense(@PathVariable Long eventId,
                                       @PathVariable Long expenseId) {
        return ExpenseApiMapper.toExpenseDetailDto(expenseService.getExpenseDetails(eventId, expenseId, currentUser()));
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long eventId,
                              @PathVariable Long expenseId) {
        expenseService.deleteExpense(eventId, expenseId, currentUser());
    }

    private User currentUser() {
        return apiUserProvider.getCurrentOnboardedUser();
    }
}
