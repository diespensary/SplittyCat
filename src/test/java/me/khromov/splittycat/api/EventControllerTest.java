package me.khromov.splittycat.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.security.filter.TmaAuthFilter;
import me.khromov.splittycat.service.BalanceService;
import me.khromov.splittycat.service.EventService;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.UserService;
import me.khromov.splittycat.service.dto.BalanceEntry;
import me.khromov.splittycat.service.dto.MyBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = EventController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TmaAuthFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private ParticipantService participantService;

    @MockitoBean
    private BalanceService balanceService;

    @MockitoBean
    private ParticipantRepository participantRepository;

    private User onboardedUser() {
        User u = new User();
        u.setId(1L);
        u.setTgId(100L);
        u.setUsername("alice");
        u.setRegistrationStep(RegistrationStep.NONE);
        u.setOnboarded(true);
        return u;
    }

    @Test
    @DisplayName("POST /api/events creates a new event")
    void createEvent_createsEvent() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event event = new Event();
        event.setId(10L);
        event.setTitle("Trip");
        event.setInviteCode("ABCDEF");
        event.setOwnerUser(user);

        when(eventService.createEvent(eq("Trip"), eq(user))).thenReturn(event);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Trip\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Trip"))
                .andExpect(jsonPath("$.inviteCode").value("ABCDEF"));
    }

    @Test
    @DisplayName("GET /api/events lists accessible events")
    void listEvents_returnsAccessibleEvents() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event e1 = new Event();
        e1.setId(1L);
        e1.setTitle("Trip");
        e1.setInviteCode("CODE1");
        e1.setOwnerUser(user);

        Event e2 = new Event();
        e2.setId(2L);
        e2.setTitle("Dinner");
        e2.setInviteCode("CODE2");
        e2.setOwnerUser(user);

        when(eventService.getEventsForUser(user)).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Trip"))
                .andExpect(jsonPath("$[1].inviteCode").value("CODE2"));
    }

    @Test
    @DisplayName("GET /api/events/{id} returns event details")
    void getEvent_returnsEvent() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event event = new Event();
        event.setId(5L);
        event.setTitle("Picnic");
        event.setInviteCode("INV123");
        event.setOwnerUser(user);

        when(eventService.requireEventAccessible(eq(5L), eq(user))).thenReturn(event);

        mockMvc.perform(get("/api/events/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Picnic"))
                .andExpect(jsonPath("$.inviteCode").value("INV123"));
    }

    @Test
    @DisplayName("DELETE /api/events/{id} deletes event")
    void deleteEvent_deletes() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        mockMvc.perform(delete("/api/events/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/events/join returns unlinked participants when not joined")
    void join_returnsUnlinked() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event event = new Event();
        event.setId(3L);
        event.setTitle("Meetup");
        event.setInviteCode("INVXYZ");
        event.setOwnerUser(user);

        when(eventService.findByInviteCode("INVXYZ")).thenReturn(event);
        when(participantRepository.findByEventAndLinkedUser(event, user)).thenReturn(Optional.empty());

        Participant p1 = new Participant();
        p1.setId(11L);
        p1.setName("Bob");

        Participant p2 = new Participant();
        p2.setId(12L);
        p2.setName("Charlie");

        when(participantRepository.findUnlinkedByEventId(3L)).thenReturn(List.of(p1, p2));

        mockMvc.perform(post("/api/events/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"INVXYZ\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyJoined").value(false))
                .andExpect(jsonPath("$.unlinkedParticipants[0].name").value("Bob"));
    }

    @Test
    @DisplayName("POST /api/events/join returns alreadyJoined when user is linked")
    void join_returnsAlreadyJoined() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event event = new Event();
        event.setId(4L);
        event.setTitle("Party");
        event.setInviteCode("INVABC");
        event.setOwnerUser(user);

        when(eventService.findByInviteCode("INVABC")).thenReturn(event);

        Participant me = new Participant();
        me.setId(99L);
        me.setName("Alice");
        me.setLinkedUser(user);

        when(participantRepository.findByEventAndLinkedUser(event, user)).thenReturn(Optional.of(me));

        mockMvc.perform(post("/api/events/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"INVABC\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyJoined").value(true))
                .andExpect(jsonPath("$.myParticipantId").value(99))
                .andExpect(jsonPath("$.unlinkedParticipants").isEmpty());
    }

    @Test
    @DisplayName("POST /api/events/join/claim links user and returns event")
    void claim_linksParticipant() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Event event = new Event();
        event.setId(8L);
        event.setTitle("Workshop");
        event.setInviteCode("WORKSH");
        event.setOwnerUser(user);

        Participant p = new Participant();
        p.setId(20L);
        p.setEvent(event);
        p.setName("Dave");
        p.setLinkedUser(user);

        when(participantService.claimParticipant("WORKSH", 20L, user)).thenReturn(p);

        mockMvc.perform(post("/api/events/join/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"WORKSH\",\"participantId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.title").value("Workshop"));
    }

    @Test
    @DisplayName("GET /api/events/{id}/my-balance returns debt summary")
    void myBalance_returnsBalance() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        BalanceEntry oweEntry = new BalanceEntry(2L, "Bob", "EUR", new BigDecimal("10.00"));
        BalanceEntry owedEntry = new BalanceEntry(3L, "Carol", "EUR", new BigDecimal("5.00"));
        MyBalance mb = new MyBalance(1L, List.of(oweEntry), List.of(owedEntry));

        when(balanceService.getMyBalance(6L, user)).thenReturn(mb);

        mockMvc.perform(get("/api/events/6/my-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myParticipantId").value(1))
                .andExpect(jsonPath("$.youOwe[0].participantId").value(2))
                .andExpect(jsonPath("$.oweYou[0].amount").value(5.00));
    }
}
