package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.CreateParticipantRequest;
import me.khromov.splittycat.api.dto.ParticipantDto;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events/{eventId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final ParticipantService participantService;

    @GetMapping
    public List<ParticipantDto> listParticipants(@PathVariable Long eventId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        List<Participant> participants = participantService.getParticipants(eventId, user);
        return participants.stream()
                .map(p -> new ParticipantDto(p.getId(), p.getName(), p.getLinkedUser() != null))
                .collect(Collectors.toList());
    }

    @PostMapping
    public ParticipantDto addParticipant(@PathVariable Long eventId,
                                         @Valid @RequestBody CreateParticipantRequest request) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Participant participant = participantService.addParticipant(eventId, request.name(), user);
        return new ParticipantDto(participant.getId(), participant.getName(), false);
    }

    @DeleteMapping("/{participantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteParticipant(@PathVariable Long eventId,
                                  @PathVariable Long participantId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        participantService.deleteParticipant(eventId, participantId, user);
    }
}
