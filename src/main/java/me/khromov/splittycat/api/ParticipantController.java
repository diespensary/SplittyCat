package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.ParticipantDto;
import me.khromov.splittycat.api.mapper.ParticipantApiMapper;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.dto.CreateParticipantCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ApiUserProvider apiUserProvider;
    private final ParticipantService participantService;

    @GetMapping
    public List<ParticipantDto> listParticipants(@PathVariable Long eventId) {
        return ParticipantApiMapper.toParticipantDtos(participantService.getParticipants(eventId, currentUser()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipantDto addParticipant(@PathVariable Long eventId,
                                         @Valid @RequestBody CreateParticipantCommand command) {
        return ParticipantApiMapper.toParticipantDto(participantService.addParticipant(eventId, command, currentUser()));
    }

    @DeleteMapping("/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteParticipant(@PathVariable Long eventId,
                                  @PathVariable Long participantId) {
        participantService.deleteParticipant(eventId, participantId, currentUser());
    }

    private User currentUser() {
        return apiUserProvider.getCurrentOnboardedUser();
    }
}
