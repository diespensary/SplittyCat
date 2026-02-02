package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.ParticipantShare;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantShareRepository extends JpaRepository<ParticipantShare, Long> {

    List<ParticipantShare> findByExpenseId(Long expenseId);
}
