package me.khromov.splittycat.service.access;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventAccessPolicy {

    private static final String EVENT_ACCESS_DENIED_MESSAGE = "Не участник этого события";

    private final EventQueryService eventQueryService;

    public Event requireAccessible(Event event, User user) {
        if (!event.isOwnedBy(user) && eventQueryService.findLinkedParticipant(event, user).isEmpty()) {
            throw new ForbiddenException(EVENT_ACCESS_DENIED_MESSAGE);
        }
        return event;
    }

    public Participant requireLinkedParticipant(Event event, User user) {
        return eventQueryService.findLinkedParticipant(event, user)
                .orElseThrow(() -> new ForbiddenException(EVENT_ACCESS_DENIED_MESSAGE));
    }

    public boolean isOwner(Event event, User user) {
        return event.isOwnedBy(user);
    }
}
