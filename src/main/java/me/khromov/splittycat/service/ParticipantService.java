package me.khromov.splittycat.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ConflictException;
import me.khromov.splittycat.common.util.InputNormalizer;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import me.khromov.splittycat.service.access.EventQueryService;
import me.khromov.splittycat.service.dto.ClaimParticipantCommand;
import me.khromov.splittycat.service.dto.CreateParticipantCommand;
import me.khromov.splittycat.service.policy.ParticipantDeletionPolicy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Service
@Validated
@RequiredArgsConstructor
public class ParticipantService {

    private final ParticipantRepository participantRepository;
    private final EventQueryService eventQueryService;
    private final EventAccessPolicy eventAccessPolicy;
    private final ParticipantDeletionPolicy participantDeletionPolicy;

    public List<Participant> getParticipants(Long eventId, User user) {
        return participantRepository.findByEventId(requireAccessibleEvent(eventId, user).getId());
    }

    public List<Participant> getUnlinkedParticipants(Long eventId, User user) {
        return participantRepository.findUnlinkedByEventId(requireAccessibleEvent(eventId, user).getId());
    }

    @Transactional
    public Participant addParticipant(Long eventId, @Valid CreateParticipantCommand command, User user) {
        Event event = requireAccessibleEvent(eventId, user);
        ensureUniqueParticipantName(event.getId(), command.name());
        return participantRepository.save(createFreeParticipant(event, command.name(), user));
    }

    @Transactional
    public void deleteParticipant(Long eventId, Long participantId, User user) {
        Event event = requireAccessibleEvent(eventId, user);
        Participant participant = eventQueryService.requireParticipant(event, participantId, "Участник не найден");

        participantDeletionPolicy.validate(event, participant, user);

        try {
            participantRepository.delete(participant);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Нельзя удалить участника, который участвует в тратах");
        }
    }

    @Transactional
    public Participant claimParticipant(@Valid ClaimParticipantCommand command, User user) {
        Event event = eventQueryService.requireByInviteCode(command.inviteCode());
        ensureUserNotJoinedYet(event, user);

        Participant participant = command.claimsExistingParticipant()
                ? claimExistingParticipant(event, command.participantId())
                : createParticipantForClaim(event, command.participantName(), user);

        participant.linkTo(user);
        return participantRepository.save(participant);
    }

    private Event requireAccessibleEvent(Long eventId, User user) {
        return eventAccessPolicy.requireAccessible(eventQueryService.requireById(eventId), user);
    }

    private Participant createFreeParticipant(Event event, String participantName, User user) {
        return Participant.freeSlot(
                event,
                user,
                participantName,
                InputNormalizer.lowercase(participantName)
        );
    }

    private Participant claimExistingParticipant(Event event, Long participantId) {
        Participant participant = eventQueryService.requireParticipant(event, participantId, "Участник не найден");
        if (participant.isLinked()) {
            throw new ConflictException("Участник уже присоединился");
        }
        return participant;
    }

    private Participant createParticipantForClaim(Event event, String participantName, User user) {
        ensureUniqueParticipantName(event.getId(), participantName);
        return createFreeParticipant(event, participantName, user);
    }

    private void ensureUserNotJoinedYet(Event event, User user) {
        if (eventQueryService.findLinkedParticipant(event, user).isPresent()) {
            throw new ConflictException("Пользователь уже присоединился к этому событию");
        }
    }

    private void ensureUniqueParticipantName(Long eventId, String name) {
        if (participantRepository.existsByEventIdAndNormalizedName(eventId, InputNormalizer.lowercase(name))) {
            throw new ConflictException("Участник с таким именем уже существует");
        }
    }
}
