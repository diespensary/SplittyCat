package me.khromov.splittycat.service.policy;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ConflictException;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.access.EventAccessPolicy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantDeletionPolicy {

    private final EventAccessPolicy accessPolicy;

    public void validate(Event event, Participant participant, User user) {
        boolean owner = accessPolicy.isOwner(event, user);
        boolean creator = participant.isCreatedBy(user);
        boolean deletingOwnOwnerSlot = owner && participant.isLinkedTo(user);

        if (deletingOwnOwnerSlot) {
            throw new ConflictException("Владелец события не может удалить свой слот участника");
        }
        if (!owner && !creator) {
            throw new ForbiddenException("Вы можете удалить только созданного вами участника");
        }
    }
}
