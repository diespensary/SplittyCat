package me.khromov.splittycat.api.mapper;

import me.khromov.splittycat.api.dto.ParticipantDto;
import me.khromov.splittycat.domain.entity.Participant;

import java.util.List;

public final class ParticipantApiMapper {

    private ParticipantApiMapper() {
    }

    public static ParticipantDto toParticipantDto(Participant participant) {
        return new ParticipantDto(participant.getId(), participant.getName(), participant.isLinked());
    }

    public static List<ParticipantDto> toParticipantDtos(List<Participant> participants) {
        return participants.stream()
                .map(ParticipantApiMapper::toParticipantDto)
                .toList();
    }
}
