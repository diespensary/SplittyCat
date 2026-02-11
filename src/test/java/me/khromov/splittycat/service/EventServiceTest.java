package me.khromov.splittycat.service;

import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipantRepository participantRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository, participantRepository);
    }

    @Test
    void createEvent_createsOwnerParticipantAndTrimsTitle() {
        User owner = new User();
        owner.setId(10L);
        owner.setUsername("Alice");

        when(eventRepository.findByInviteCode(anyString())).thenReturn(Optional.empty());
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(55L);
            return e;
        });
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event created = eventService.createEvent("  Trip to SPB  ", owner);

        assertEquals(55L, created.getId());
        assertEquals("Trip to SPB", created.getTitle());
        assertNotNull(created.getInviteCode());
        assertEquals(6, created.getInviteCode().length());

        ArgumentCaptor<Participant> participantCaptor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(participantCaptor.capture());
        Participant ownerParticipant = participantCaptor.getValue();

        assertEquals("Alice", ownerParticipant.getName());
        assertEquals("alice", ownerParticipant.getNormalizedName());
        assertSame(owner, ownerParticipant.getLinkedUser());
        assertSame(owner, ownerParticipant.getCreatedByUser());
        assertSame(created, ownerParticipant.getEvent());
    }

    @Test
    void deleteEvent_throwsForbidden_whenUserIsNotOwner() {
        User owner = new User();
        owner.setId(1L);

        User outsider = new User();
        outsider.setId(2L);

        Event event = new Event();
        event.setId(100L);
        event.setOwnerUser(owner);

        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> eventService.deleteEvent(100L, outsider));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(eventRepository, never()).delete(any(Event.class));
    }
}