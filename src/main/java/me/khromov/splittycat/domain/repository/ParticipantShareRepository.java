package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.ParticipantShare;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ParticipantShareRepository extends JpaRepository<ParticipantShare, Long> {

    @EntityGraph(attributePaths = {"participant", "expense"})
    List<ParticipantShare> findByExpenseId(Long expenseId);

    @EntityGraph(attributePaths = {"participant", "expense"})
    List<ParticipantShare> findByExpenseIdIn(Collection<Long> expenseIds);
}
