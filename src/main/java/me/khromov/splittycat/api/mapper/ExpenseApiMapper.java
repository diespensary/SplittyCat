package me.khromov.splittycat.api.mapper;

import me.khromov.splittycat.api.dto.ExpenseDetailDto;
import me.khromov.splittycat.api.dto.ExpenseSummaryDto;
import me.khromov.splittycat.api.dto.ParticipantDto;
import me.khromov.splittycat.api.dto.ShareDto;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.service.dto.ExpenseDetails;

import java.util.List;

public final class ExpenseApiMapper {

    private ExpenseApiMapper() {
    }

    public static ExpenseSummaryDto toExpenseSummaryDto(Expense expense) {
        return new ExpenseSummaryDto(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency().getCode(),
                expense.getExpenseDate(),
                expense.getPayerParticipant().getName()
        );
    }

    public static List<ExpenseSummaryDto> toExpenseSummaryDtos(List<Expense> expenses) {
        return expenses.stream()
                .map(ExpenseApiMapper::toExpenseSummaryDto)
                .toList();
    }

    public static ExpenseDetailDto toExpenseDetailDto(ExpenseDetails expenseDetails) {
        Expense expense = expenseDetails.expense();
        Participant payer = expense.getPayerParticipant();
        ParticipantDto payerDto = ParticipantApiMapper.toParticipantDto(payer);

        return new ExpenseDetailDto(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency().getCode(),
                expense.getExpenseDate(),
                payerDto,
                expenseDetails.shares().stream()
                        .map(ExpenseApiMapper::toShareDto)
                        .toList()
        );
    }

    private static ShareDto toShareDto(ParticipantShare share) {
        return new ShareDto(share.getParticipant().getId(), share.getAmount(), share.getDescription());
    }
}
