package me.khromov.splittycat.service.access;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.NotFoundException;
import me.khromov.splittycat.common.util.InputNormalizer;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class EventQueryService {

    private final EventRepository eventRepository;
    private final ParticipantRepository participantRepository;

    public Event requireById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    public Event requireByInviteCode(@NotBlank String inviteCode) {
        return eventRepository.findByInviteCode(InputNormalizer.trim(inviteCode))
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    public Participant requireParticipant(Event event, Long participantId, String message) {
        return participantRepository.findByIdAndEventId(participantId, event.getId())
                .orElseThrow(() -> new NotFoundException(message));
    }

    public Optional<Participant> findLinkedParticipant(Event event, User user) {
        return participantRepository.findByEventAndLinkedUser(event, user);
    }
}
