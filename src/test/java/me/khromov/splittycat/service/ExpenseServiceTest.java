package me.khromov.splittycat.service;

import me.khromov.splittycat.domain.entity.Currency;
import me.khromov.splittycat.domain.entity.Event;
import me.khromov.splittycat.domain.entity.Participant;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.CurrencyRepository;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private ParticipantShareRepository participantShareRepository;
    @Mock private CurrencyRepository currencyRepository;

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        expenseService = new ExpenseService(
                eventRepository,
                expenseRepository,
                participantRepository,
                participantShareRepository,
                currencyRepository
        );
    }

    @Test
    void createExpense_throws422_whenSharesSumDoesNotMatchTotalAmount() {
        User user = new User();
        user.setId(1L);

        Event event = new Event();
        event.setId(10L);
        event.setOwnerUser(user);

        Participant payer = new Participant();
        payer.setId(100L);

        Currency currency = new Currency();
        currency.setId(1L);
        currency.setCode("RUB");

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(currencyRepository.findByCodeIgnoreCase("RUB")).thenReturn(Optional.of(currency));
        when(participantRepository.findByIdAndEventId(100L, 10L)).thenReturn(Optional.of(payer));
        when(participantRepository.findByEventAndLinkedUser(event, user)).thenReturn(Optional.of(payer));

        List<ExpenseService.ShareInput> shares = List.of(
                new ExpenseService.ShareInput(100L, new BigDecimal("50.00"), null)
        );

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                expenseService.createExpense(
                        10L,
                        "Taxi",
                        new BigDecimal("100.00"),
                        "RUB",
                        LocalDate.now(),
                        100L,
                        shares,
                        user
                )
        );

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, ex.getStatusCode());
        assertEquals("Сумма долей должна равняться общей сумме", ex.getReason());
    }
}