package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByEventId(Long eventId);

    Optional<Expense> findByIdAndEventId(Long id, Long eventId);
}
