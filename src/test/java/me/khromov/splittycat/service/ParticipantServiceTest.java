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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipantServiceTest {

    @Mock private ParticipantRepository participantRepository;
    @Mock private EventRepository eventRepository;

    private ParticipantService participantService;

    @BeforeEach
    void setUp() {
        participantService = new ParticipantService(participantRepository, eventRepository);
    }

    @Test
    void addParticipant_throwsConflict_whenNameAlreadyExists() {
        User user = new User();
        user.setId(1L);

        Event event = new Event();
        event.setId(20L);
        event.setOwnerUser(user);

        when(eventRepository.findById(20L)).thenReturn(Optional.of(event));
        when(participantRepository.existsByEventIdAndNormalizedName(20L, "bob")).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> participantService.addParticipant(20L, " Bob ", user));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(participantRepository, never()).save(any(Participant.class));
    }

    @Test
    void claimParticipant_createsNewAndLinksUser_whenParticipantIdNotProvided() {
        User user = new User();
        user.setId(7L);

        Event event = new Event();
        event.setId(31L);

        when(eventRepository.findByInviteCode("ABC123")).thenReturn(Optional.of(event));
        when(participantRepository.findByEventAndLinkedUser(event, user)).thenReturn(Optional.empty());
        when(participantRepository.existsByEventIdAndNormalizedName(31L, "new person")).thenReturn(false);
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Participant p = participantService.claimParticipant("ABC123", null, " New Person ", user);

        assertEquals("New Person", p.getName());
        assertEquals("new person", p.getNormalizedName());
        assertSame(user, p.getLinkedUser());
        assertSame(user, p.getCreatedByUser());
        assertSame(event, p.getEvent());

        ArgumentCaptor<Participant> captor = ArgumentCaptor.forClass(Participant.class);
        verify(participantRepository).save(captor.capture());
        assertSame(user, captor.getValue().getLinkedUser());
    }

    @Test
    void claimParticipant_throwsConflict_whenUserAlreadyLinkedToEvent() {
        User user = new User();
        user.setId(11L);

        Event event = new Event();
        event.setId(44L);

        Participant existing = new Participant();
        existing.setId(101L);
        existing.setEvent(event);
        existing.setLinkedUser(user);

        when(eventRepository.findByInviteCode("QWERTY")).thenReturn(Optional.of(event));
        when(participantRepository.findByEventAndLinkedUser(event, user)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> participantService.claimParticipant("QWERTY", null, "Nick", user));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }
}