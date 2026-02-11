package me.khromov.splittycat.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.*;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.service.BalanceService;
import me.khromov.splittycat.service.EventService;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final CurrentUser currentUser;
    private final UserService userService;
    private final EventService eventService;
    private final ParticipantService participantService;
    private final BalanceService balanceService;
    private final ParticipantRepository participantRepository;

    @PostMapping
    public EventDto createEvent(@Valid @RequestBody CreateEventRequest request) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Event event = eventService.createEvent(request.title(), user);
        return new EventDto(event.getId(), event.getTitle(), event.getInviteCode());
    }

    @GetMapping
    public List<EventDto> listEvents() {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        return eventService.getEventsForUser(user).stream()
                .map(e -> new EventDto(e.getId(), e.getTitle(), e.getInviteCode()))
                .collect(Collectors.toList());
    }

    @GetMapping("/{eventId}")
    public EventDto getEvent(@PathVariable Long eventId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Event event = eventService.requireEventAccessible(eventId, user);
        return new EventDto(event.getId(), event.getTitle(), event.getInviteCode());
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvent(@PathVariable Long eventId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        eventService.deleteEvent(eventId, user);
    }

    @PostMapping("/join")
    public JoinResponse join(@Valid @RequestBody JoinRequestBody body) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        String inviteCode = body.inviteCode();
        Event event = eventService.findByInviteCode(inviteCode);
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        Participant linked = participantRepository.findByEventAndLinkedUser(event, user).orElse(null);
        boolean alreadyJoined = linked != null;
        Long myParticipantId = alreadyJoined ? linked.getId() : null;
        List<ParticipantDto> unlinked = alreadyJoined ? List.of() :
                participantRepository.findUnlinkedByEventId(event.getId()).stream()
                        .map(p -> new ParticipantDto(p.getId(), p.getName(), false))
                        .toList();
        return new JoinResponse(event.getId(), event.getTitle(), event.getInviteCode(),
                alreadyJoined, myParticipantId, unlinked);
    }

    @PostMapping("/join/claim")
    public EventDto claim(@Valid @RequestBody ClaimRequestBody body) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        Participant p = participantService.claimParticipant(body.inviteCode(), body.participantId(), body.participantName(), user);
        Event event = p.getEvent();
        return new EventDto(event.getId(), event.getTitle(), event.getInviteCode());
    }

    @GetMapping("/{eventId}/my-balance")
    public BalanceResponse myBalance(@PathVariable Long eventId) {
        User user = userService.requireOnboardedUser(currentUser.tgId());
        var balance = balanceService.getMyBalance(eventId, user);

        List<BalanceEntryDto> youOwe = balance.youOwe().stream()
                .map(e -> new BalanceEntryDto(e.participantId(), e.participantName(), e.currencyCode(), e.amount()))
                .toList();

        List<BalanceEntryDto> oweYou = balance.oweYou().stream()
                .map(e -> new BalanceEntryDto(e.participantId(), e.participantName(), e.currencyCode(), e.amount()))
                .toList();

        return new BalanceResponse(balance.myParticipantId(), youOwe, oweYou);
    }

}
