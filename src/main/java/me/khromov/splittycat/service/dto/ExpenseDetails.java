package me.khromov.splittycat.service.dto;

import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.ParticipantShare;

import java.util.List;

public record ExpenseDetails(Expense expense, List<ParticipantShare> shares) {
}
