package me.khromov.splittycat.domain.repository;

import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    @EntityGraph(attributePaths = {"linkedUser", "createdByUser"})
    List<Participant> findByEventId(Long eventId);

    List<Participant> findByEventIdAndIdIn(Long eventId, Collection<Long> participantIds);

    @EntityGraph(attributePaths = {"linkedUser", "createdByUser"})
    Optional<Participant> findByIdAndEventId(Long id, Long eventId);

    @EntityGraph(attributePaths = "linkedUser")
    Optional<Participant> findByEventAndLinkedUser(Event event, User linkedUser);

    boolean existsByEventIdAndNormalizedName(Long eventId, String normalizedName);

    @Query("select p from Participant p where p.event.id = :eventId and p.linkedUser is null")
    List<Participant> findUnlinkedByEventId(@Param("eventId") Long eventId);
}
