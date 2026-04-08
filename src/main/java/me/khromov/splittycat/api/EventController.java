package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.BalanceResponse;
import me.khromov.splittycat.api.dto.EventDto;
import me.khromov.splittycat.api.dto.JoinResponse;
import me.khromov.splittycat.api.mapper.BalanceApiMapper;
import me.khromov.splittycat.api.mapper.EventApiMapper;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.service.BalanceService;
import me.khromov.splittycat.service.EventService;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.dto.ClaimParticipantCommand;
import me.khromov.splittycat.service.dto.CreateEventCommand;
import me.khromov.splittycat.service.dto.JoinEventCommand;
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
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final ApiUserProvider apiUserProvider;
    private final EventService eventService;
    private final ParticipantService participantService;
    private final BalanceService balanceService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto createEvent(@Valid @RequestBody CreateEventCommand command) {
        return EventApiMapper.toEventDto(eventService.createEvent(command, currentUser()));
    }

    @GetMapping
    public List<EventDto> listMyEvents() {
        return EventApiMapper.toEventDtos(eventService.getEventsForUser(currentUser()));
    }

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable Long eventId) {
        return EventApiMapper.toEventDto(eventService.requireEventAccessible(eventId, currentUser()));
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long eventId) {
        eventService.deleteEvent(eventId, currentUser());
    }

    @PostMapping("/join")
    public JoinResponse join(@Valid @RequestBody JoinEventCommand command) {
        return EventApiMapper.toJoinResponse(eventService.getJoinPreview(command, currentUser()));
    }

    @PostMapping("/join/claim")
    public EventDto claim(@Valid @RequestBody ClaimParticipantCommand command) {
        Participant participant = participantService.claimParticipant(command, currentUser());
        return EventApiMapper.toEventDto(participant.getEvent());
    }

    @GetMapping("/{eventId}/my-balance")
    public BalanceResponse myBalance(@PathVariable Long eventId) {
        return BalanceApiMapper.toBalanceResponse(balanceService.getMyBalance(eventId, currentUser()));
    }

    private User currentUser() {
        return apiUserProvider.getCurrentOnboardedUser();
    }
}
