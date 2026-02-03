package me.khromov.splittycat.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.security.filter.TmaAuthFilter;
import me.khromov.splittycat.service.ParticipantService;
import me.khromov.splittycat.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ParticipantController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TmaAuthFilter.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ParticipantControllerTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ParticipantService participantService;

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
    @DisplayName("GET /api/events/{id}/participants returns list of participants")
    void listParticipants_returnsList() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Participant p1 = new Participant();
        p1.setId(2L);
        p1.setName("Bob");

        Participant p2 = new Participant();
        p2.setId(3L);
        p2.setName("Carol");
        p2.setLinkedUser(user);

        when(participantService.getParticipants(eq(5L), eq(user))).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/api/events/5/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].name").value("Bob"))
                .andExpect(jsonPath("$[0].linked").value(false))
                .andExpect(jsonPath("$[1].linked").value(true));
    }

    @Test
    @DisplayName("POST /api/events/{id}/participants adds a participant")
    void addParticipant_adds() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        Participant newP = new Participant();
        newP.setId(4L);
        newP.setName("Dave");

        when(participantService.addParticipant(eq(7L), eq("Dave"), eq(user))).thenReturn(newP);

        mockMvc.perform(post("/api/events/7/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dave\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.name").value("Dave"))
                .andExpect(jsonPath("$.linked").value(false));
    }

    @Test
    @DisplayName("DELETE /api/events/{id}/participants/{pid} deletes participant")
    void deleteParticipant_deletes() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        mockMvc.perform(delete("/api/events/9/participants/3"))
                .andExpect(status().isNoContent());
    }
}
