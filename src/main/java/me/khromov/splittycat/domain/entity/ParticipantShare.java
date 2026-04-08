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
import java.time.OffsetDateTime;

@Entity
@Table(name = "participant_shares")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "participant_marked_paid_at")
    private OffsetDateTime participantMarkedPaidAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    public static ParticipantShare create(Expense expense, Participant participant, BigDecimal amount, String description) {
        ParticipantShare share = new ParticipantShare();
        share.expense = expense;
        share.participant = participant;
        share.amount = amount;
        share.description = description;
        return share;
    }
}
