package me.khromov.splittycat.service.dto;

import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;

import java.util.List;

public record JoinPreview(Event event, Participant linkedParticipant, List<Participant> unlinkedParticipants) {

    public static JoinPreview alreadyJoined(Event event, Participant linkedParticipant) {
        return new JoinPreview(event, linkedParticipant, List.of());
    }

    public static JoinPreview available(Event event, List<Participant> unlinkedParticipants) {
        return new JoinPreview(event, null, unlinkedParticipants);
    }

    public boolean alreadyJoined() {
        return linkedParticipant != null;
    }

    public Long myParticipantId() {
        return linkedParticipant == null ? null : linkedParticipant.getId();
    }
}
