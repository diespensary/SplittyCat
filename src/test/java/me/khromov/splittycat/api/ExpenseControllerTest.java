package me.khromov.splittycat.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import me.khromov.splittycat.domain.entity.Currency;
import me.khromov.splittycat.domain.entity.Expense;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.ParticipantShare;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.security.filter.TmaAuthFilter;
import me.khromov.splittycat.service.ExpenseService;
import me.khromov.splittycat.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ExpenseController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TmaAuthFilter.class)
        }
)
class ExpenseControllerTest {

    private final ObjectMapper objectMapper =
            JsonMapper.builder().findAndAddModules().build();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUser currentUser;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private ParticipantShareRepository participantShareRepository;

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
    @DisplayName("GET /api/events/{id}/expenses returns expenses")
    void listExpenses_returnsList() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);
        Expense e1 = new Expense();
        e1.setId(2L);
        e1.setTitle("Dinner");
        e1.setAmount(new BigDecimal("30.00"));
        Currency cur = new Currency();
        cur.setCode("EUR");
        e1.setCurrency(cur);
        e1.setExpenseDate(LocalDate.of(2024, 1, 1));
        Participant payer = new Participant();
        payer.setName("Bob");
        e1.setPayerParticipant(payer);
        when(expenseService.getExpenses(eq(5L), eq(user))).thenReturn(List.of(e1));

        mockMvc.perform(get("/api/events/5/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].title").value("Dinner"))
                .andExpect(jsonPath("$[0].currencyCode").value("EUR"));
    }

    @Test
    @DisplayName("POST /api/events/{id}/expenses creates expense")
    void createExpense_creates() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);
        Expense saved = new Expense();
        saved.setId(10L);
        saved.setTitle("Lunch");
        saved.setAmount(new BigDecimal("15.00"));
        Currency cur = new Currency();
        cur.setCode("USD");
        saved.setCurrency(cur);
        saved.setExpenseDate(LocalDate.of(2024, 2, 1));
        Participant payer = new Participant();
        payer.setName("Carol");
        saved.setPayerParticipant(payer);
        when(expenseService.createExpense(eq(7L), eq("Lunch"), eq(new BigDecimal("15.00")),
                eq("USD"), eq(LocalDate.of(2024, 2, 1)), eq(5L), anyMap(), eq(user)))
                .thenReturn(saved);

        String json = "{"
                + "\"title\":\"Lunch\","
                + "\"amount\":15.00,"
                + "\"currencyCode\":\"USD\","
                + "\"expenseDate\":\"2024-02-01\","
                + "\"payerParticipantId\":5,"
                + "\"shares\":[{\"participantId\":5,\"amount\":15.00}]"
                + "}";

        mockMvc.perform(post("/api/events/7/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lunch"))
                .andExpect(jsonPath("$.amount").value(15.00))
                .andExpect(jsonPath("$.currencyCode").value("USD"))
                .andExpect(jsonPath("$.payerName").value("Carol"));
    }

    @Test
    @DisplayName("GET /api/events/{id}/expenses/{expId} returns detailed expense")
    void getExpense_returnsDetail() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);
        Expense exp = new Expense();
        exp.setId(3L);
        exp.setTitle("Brunch");
        exp.setAmount(new BigDecimal("12.00"));
        Currency cur = new Currency();
        cur.setCode("GBP");
        exp.setCurrency(cur);
        exp.setExpenseDate(LocalDate.of(2024, 3, 1));
        Participant payer = new Participant();
        payer.setId(4L);
        payer.setName("Dan");
        payer.setLinkedUser(user);
        exp.setPayerParticipant(payer);
        when(expenseService.getExpense(eq(8L), eq(3L), eq(user))).thenReturn(exp);
        ParticipantShare share1 = new ParticipantShare();
        share1.setId(1L);
        Participant sp1 = new Participant();
        sp1.setId(4L);
        share1.setParticipant(sp1);
        share1.setAmount(new BigDecimal("6.00"));
        ParticipantShare share2 = new ParticipantShare();
        share2.setId(2L);
        Participant sp2 = new Participant();
        sp2.setId(5L);
        sp2.setName("Eve");
        share2.setParticipant(sp2);
        share2.setAmount(new BigDecimal("6.00"));
        when(participantShareRepository.findByExpenseId(3L)).thenReturn(List.of(share1, share2));

        mockMvc.perform(get("/api/events/8/expenses/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.payer.id").value(4))
                .andExpect(jsonPath("$.shares[1].participantId").value(5))
                .andExpect(jsonPath("$.shares[1].amount").value(6.00));
    }

    @Test
    @DisplayName("DELETE /api/events/{id}/expenses/{expId} deletes expense")
    void deleteExpense_deletes() throws Exception {
        User user = onboardedUser();
        when(currentUser.tgId()).thenReturn(user.getTgId());
        when(userService.requireOnboardedUser(user.getTgId())).thenReturn(user);

        mockMvc.perform(delete("/api/events/9/expenses/2"))
                .andExpect(status().isNoContent());
    }
}
