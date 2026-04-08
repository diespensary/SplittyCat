package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.Expense;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @EntityGraph(attributePaths = {"currency", "ownerUser", "payerParticipant", "payerParticipant.linkedUser"})
    List<Expense> findByEventId(Long eventId);

    @EntityGraph(attributePaths = {"currency", "ownerUser", "payerParticipant", "payerParticipant.linkedUser"})
    Optional<Expense> findByIdAndEventId(Long id, Long eventId);
}
