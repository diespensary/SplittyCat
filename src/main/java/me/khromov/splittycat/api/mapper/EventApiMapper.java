package me.khromov.splittycat.api.mapper;

import me.khromov.splittycat.api.dto.EventDto;
import me.khromov.splittycat.api.dto.JoinResponse;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.service.dto.JoinPreview;

import java.util.List;

public final class EventApiMapper {

    private EventApiMapper() {
    }

    public static EventDto toEventDto(Event event) {
        return new EventDto(event.getId(), event.getTitle(), event.getInviteCode());
    }

    public static List<EventDto> toEventDtos(List<Event> events) {
        return events.stream()
                .map(EventApiMapper::toEventDto)
                .toList();
    }

    public static JoinResponse toJoinResponse(JoinPreview preview) {
        Event event = preview.event();
        return new JoinResponse(
                event.getId(),
                event.getTitle(),
                event.getInviteCode(),
                preview.alreadyJoined(),
                preview.myParticipantId(),
                ParticipantApiMapper.toParticipantDtos(preview.unlinkedParticipants())
        );
    }
}
