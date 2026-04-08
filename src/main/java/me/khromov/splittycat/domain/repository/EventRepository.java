package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.Event;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Override
    @EntityGraph(attributePaths = "ownerUser")
    Optional<Event> findById(Long id);

    @EntityGraph(attributePaths = "ownerUser")
    Optional<Event> findByInviteCode(String inviteCode);

    List<Event> findByOwnerUserId(Long userId);

    @Query("select distinct e from Event e " +
            "left join Participant p on p.event = e and p.linkedUser.id = :userId " +
            "where e.ownerUser.id = :userId or p.id is not null")
    List<Event> findEventsForUser(@Param("userId") Long userId);
}
