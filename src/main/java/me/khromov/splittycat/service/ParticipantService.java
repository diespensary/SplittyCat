package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Имя не может быть пустым");
        }
        Event event = requireAccess(eventId, user);
        String normalized = normalizeName(name);
        if (participantRepository.existsByEventIdAndNormalizedName(eventId, normalized)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Участник с таким именем уже существует");
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Участник не найден"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean creator = p.getCreatedByUser() != null && p.getCreatedByUser().getId().equals(user.getId());
        boolean deletingOwnOwnerSlot = owner && p.getLinkedUser() != null && p.getLinkedUser().getId().equals(user.getId());
        if (deletingOwnOwnerSlot) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Владелец события не может удалить свой слот участника");
        }
        if (!owner && !creator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы можете удалить только созданного вами участника");
        }
        try {
            participantRepository.delete(p);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя удалить участника, который участвует в тратах");
        }
    }

    @Transactional
    public List<Participant> getUnlinkedParticipants(Long eventId, User user) {
        Event event = requireAccess(eventId, user);
        return participantRepository.findUnlinkedByEventId(event.getId());
    }

    @Transactional
    public Participant claimParticipant(String inviteCode, Long participantId, String participantName, User user) {
        Event event = eventRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
        boolean alreadyLinked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (alreadyLinked) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь уже присоединился к этому событию");
        }
        Participant participant;
        if (participantId != null) {
            participant = participantRepository.findByIdAndEventId(participantId, event.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Участник не найден"));
            if (participant.getLinkedUser() != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Участник уже присоединился");
            }
        } else {
            if (participantName == null || participantName.trim().isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Имя участника не может быть пустым");
            }
            String normalized = normalizeName(participantName);
            if (participantRepository.existsByEventIdAndNormalizedName(event.getId(), normalized)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Участник с таким именем уже существует");
            }
            participant = new Participant();
            participant.setEvent(event);
            participant.setName(participantName.trim());
            participant.setNormalizedName(normalized);
            participant.setCreatedByUser(user);
        }
        participant.setLinkedUser(user);
        return participantRepository.save(participant);
    }

    private Event requireAccess(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean linked = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (!owner && !linked) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Не участник этого события");
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
