package me.khromov.splittycat.api.dto;

import java.util.List;

public record JoinResponse(Long eventId,
                           String title,
                           String inviteCode,
                           boolean alreadyJoined,
                           Long myParticipantId,
                           List<ParticipantDto> unlinkedParticipants) {
}
