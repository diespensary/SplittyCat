package me.khromov.splittycat.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ConflictException;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.common.exception.UnprocessableEntityException;
import me.khromov.splittycat.common.util.InputNormalizer;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import me.khromov.splittycat.service.access.EventQueryService;
import me.khromov.splittycat.service.dto.CreateEventCommand;
import me.khromov.splittycat.service.dto.JoinEventCommand;
import me.khromov.splittycat.service.dto.JoinPreview;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.security.SecureRandom;
import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class EventService {

    private static final String INVITE_CHARACTERS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int INVITE_LENGTH = 6;
    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;
    private final EventQueryService eventQueryService;
    private final EventAccessPolicy eventAccessPolicy;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public Event createEvent(@Valid CreateEventCommand command, User ownerUser) {
        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            try {
                Event event = eventRepository.saveAndFlush(Event.create(command.title(), ownerUser, generateInviteCode()));
                participantRepository.save(createOwnerParticipant(event, ownerUser));
                return event;
            } catch (DataIntegrityViolationException exception) {
                if (attempt == MAX_CODE_GENERATION_ATTEMPTS - 1) {
                    throw new UnprocessableEntityException("Невозможно сгенерировать уникальный код приглашения");
                }
            }
        }
        throw new UnprocessableEntityException("Невозможно сгенерировать уникальный код приглашения");
    }

    @Transactional
    public void deleteEvent(Long eventId, User user) {
        Event event = eventQueryService.requireById(eventId);
        if (!eventAccessPolicy.isOwner(event, user)) {
            throw new ForbiddenException("Удалить событие может только владелец");
        }

        try {
            eventRepository.delete(event);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Нельзя удалить событие, пока в нём есть траты");
        }
    }

    public List<Event> getEventsForUser(User user) {
        return eventRepository.findEventsForUser(user.getId());
    }

    public Event requireEventAccessible(Long eventId, User user) {
        return eventAccessPolicy.requireAccessible(eventQueryService.requireById(eventId), user);
    }

    public JoinPreview getJoinPreview(@Valid JoinEventCommand command, User user) {
        Event event = eventQueryService.requireByInviteCode(command.inviteCode());
        return eventQueryService.findLinkedParticipant(event, user)
                .map(linkedParticipant -> JoinPreview.alreadyJoined(event, linkedParticipant))
                .orElseGet(() -> JoinPreview.available(event, participantRepository.findUnlinkedByEventId(event.getId())));
    }

    private Participant createOwnerParticipant(Event event, User ownerUser) {
        return Participant.ownerSlot(
                event,
                ownerUser,
                ownerUser.getUsername(),
                InputNormalizer.lowercase(ownerUser.getUsername())
        );
    }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_LENGTH);
        for (int index = 0; index < INVITE_LENGTH; index++) {
            int randomIndex = random.nextInt(INVITE_CHARACTERS.length());
            code.append(INVITE_CHARACTERS.charAt(randomIndex));
        }
        return code.toString();
    }
}
