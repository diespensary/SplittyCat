package me.khromov.splittycat.service;

import me.khromov.splittycat.domain.entity.*;
import me.khromov.splittycat.domain.repository.EventRepository;
import me.khromov.splittycat.domain.repository.ExpenseRepository;
import me.khromov.splittycat.domain.repository.ParticipantRepository;
import me.khromov.splittycat.domain.repository.ParticipantShareRepository;
import me.khromov.splittycat.service.dto.MyBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ParticipantShareRepository shareRepository;

    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService = new BalanceService(eventRepository, participantRepository, expenseRepository, shareRepository);
    }

    @Test
    void getMyBalance_aggregatesYouOweAndOweYouByParticipantAndCurrency() {
        User meUser = new User();
        meUser.setId(1L);

        Event event = new Event();
        event.setId(10L);

        Participant me = participant("Me", 100L, meUser);
        Participant alice = participant("Alice", 200L, null);

        Currency rub = new Currency();
        rub.setCode("RUB");

        Expense expenseByAlice = new Expense();
        expenseByAlice.setId(1000L);
        expenseByAlice.setCurrency(rub);
        expenseByAlice.setPayerParticipant(alice);

        Expense expenseByMe = new Expense();
        expenseByMe.setId(2000L);
        expenseByMe.setCurrency(rub);
        expenseByMe.setPayerParticipant(me);

        ParticipantShare shareMeInAliceExpense = new ParticipantShare();
        shareMeInAliceExpense.setParticipant(me);
        shareMeInAliceExpense.setAmount(new BigDecimal("300.00"));

        ParticipantShare shareAliceInMeExpense = new ParticipantShare();
        shareAliceInMeExpense.setParticipant(alice);
        shareAliceInMeExpense.setAmount(new BigDecimal("120.50"));

        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(participantRepository.findByEventAndLinkedUser(event, meUser)).thenReturn(Optional.of(me));
        when(expenseRepository.findByEventId(10L)).thenReturn(List.of(expenseByAlice, expenseByMe));
        when(shareRepository.findByExpenseId(1000L)).thenReturn(List.of(shareMeInAliceExpense));
        when(shareRepository.findByExpenseId(2000L)).thenReturn(List.of(shareAliceInMeExpense));

        MyBalance result = balanceService.getMyBalance(10L, meUser);

        assertEquals(100L, result.myParticipantId());
        assertEquals(1, result.youOwe().size());
        assertEquals(1, result.oweYou().size());

        assertEquals(200L, result.youOwe().getFirst().participantId());
        assertEquals("RUB", result.youOwe().getFirst().currencyCode());
        assertEquals(new BigDecimal("300.00"), result.youOwe().getFirst().amount());

        assertEquals(200L, result.oweYou().getFirst().participantId());
        assertEquals("RUB", result.oweYou().getFirst().currencyCode());
        assertEquals(new BigDecimal("120.50"), result.oweYou().getFirst().amount());
    }

    private static Participant participant(String name, long id, User linkedUser) {
        Participant p = new Participant();
        p.setId(id);
        p.setName(name);
        p.setLinkedUser(linkedUser);
        return p;
    }
}