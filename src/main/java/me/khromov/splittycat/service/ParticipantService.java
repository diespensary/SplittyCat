package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EventRepository eventRepository;

    @Transactional
    public List<Participant> getParticipants(Long eventId, User user) {
        Event event = requireAccess(eventId, user);
        return participantRepository.findByEventId(event.getId());
    }

    @Transactional
    public Participant addParticipant(Long eventId, String name, User user) {
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name cannot be blank");
        }
        Event event = requireAccess(eventId, user);
        String normalized = normalizeName(name);
        if (participantRepository.existsByEventIdAndNormalizedName(eventId, normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Participant with the same name already exists");
        }
        Participant p = new Participant();
        p.setEvent(event);
        p.setName(name.trim());
        p.setNormalizedName(normalized);
        p.setCreatedByUser(user);
        return participantRepository.save(p);
    }

    @Transactional
    public void deleteParticipant(Long eventId, Long participantId, User user) {
        Event event = requireAccess(eventId, user);
        Participant p = participantRepository.findByIdAndEventId(participantId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean creator = p.getCreatedByUser() != null && p.getCreatedByUser().getId().equals(user.getId());
        if (!owner && !creator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this participant");
        }
        try {
            participantRepository.delete(p);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Participant is used in expenses");
        }
    }

    @Transactional
    public List<Participant> getUnlinkedParticipants(Long eventId, User user) {
        Event event = requireAccess(eventId, user);
        return participantRepository.findUnlinkedByEventId(event.getId());
    }

    @Transactional
    public Participant claimParticipant(String inviteCode, Long participantId, User user) {
        Event event = eventRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        boolean alreadyLinked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (alreadyLinked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already joined this event");
        }
        Participant participant = participantRepository.findByIdAndEventId(participantId, event.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found"));
        if (participant.getLinkedUser() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Participant is already linked");
        }
        participant.setLinkedUser(user);
        return participantRepository.save(participant);
    }

    private Event requireAccess(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean linked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (!owner && !linked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this event");
        }
        return event;
    }

    private static String normalizeName(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase();
    }
}
