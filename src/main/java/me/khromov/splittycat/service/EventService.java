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

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final String INVITE_CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_LENGTH = 6;

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Event createEvent(String title, User ownerUser) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название не может быть пустым");
        }

        String inviteCode = null;
        for (int i = 0; i < 5; i++) {
            String code = generateInviteCode();
            if (eventRepository.findByInviteCode(code).isEmpty()) {
                inviteCode = code;
                break;
            }
        }
        if (inviteCode == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Невозможно сгенерировать уникальный код приглашения");
        }
        Event event = new Event();
        event.setTitle(title.trim());
        event.setOwnerUser(ownerUser);
        event.setInviteCode(inviteCode);
        event = eventRepository.save(event);

        // создаём участника‑владельца
        Participant participant = new Participant();
        participant.setEvent(event);
        participant.setName(ownerUser.getUsername());
        participant.setNormalizedName(normalizeName(ownerUser.getUsername()));
        participant.setLinkedUser(ownerUser);
        participant.setCreatedByUser(ownerUser);
        participantRepository.save(participant);

        return event;
    }

    @Transactional
    public List<Event> getEventsForUser(User user) {
        return eventRepository.findEventsForUser(user.getId());
    }

    @Transactional
    public Event findByInviteCode(String inviteCode) {
        if (inviteCode == null || inviteCode.isBlank()) {
            return null;
        }
        return eventRepository.findByInviteCode(inviteCode.trim()).orElse(null);
    }

    @Transactional
    public Event requireEventAccessible(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
        boolean owner = event.getOwnerUser().getId().equals(user.getId());
        boolean participant = participantRepository.findByEventAndLinkedUser(event, user).isPresent();
        if (!owner && !participant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Не участник этого события");
        }
        return event;
    }

    @Transactional
    public void deleteEvent(Long eventId, User user) {
        Event event = eventRepository.findById(eventId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Событие не найдено"));
        if (!event.getOwnerUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Удалить событие может только владелец");
        }
        try {
            eventRepository.delete(event);
            eventRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Нельзя удалить событие, пока в нём есть траты");
        }
    }

    private static String normalizeName(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase();
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_LENGTH);
        for (int i = 0; i < INVITE_LENGTH; i++) {
            int idx = random.nextInt(INVITE_CHARACTERS.length());
            sb.append(INVITE_CHARACTERS.charAt(idx));
        }
        return sb.toString();
    }
}
