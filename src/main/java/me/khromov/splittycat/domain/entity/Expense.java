package me.khromov.splittycat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id", nullable = false)
    private Currency currency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private User ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_participant_id", nullable = false)
    private Participant payerParticipant;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    public static Expense create(Event event,
                                 String title,
                                 BigDecimal amount,
                                 Currency currency,
                                 User ownerUser,
                                 Participant payerParticipant,
                                 LocalDate expenseDate) {
        Expense expense = new Expense();
        expense.event = event;
        expense.title = title;
        expense.amount = amount;
        expense.currency = currency;
        expense.ownerUser = ownerUser;
        expense.payerParticipant = payerParticipant;
        expense.expenseDate = expenseDate;
        return expense;
    }

    public boolean isCreatedBy(User user) {
        return ownerUser != null && user != null && ownerUser.hasId(user.getId());
    }

    public boolean isPaidBy(User user) {
        return payerParticipant != null && payerParticipant.isLinkedTo(user);
    }
}
