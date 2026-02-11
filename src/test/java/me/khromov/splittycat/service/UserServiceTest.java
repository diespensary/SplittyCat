package me.khromov.splittycat.service;

import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void ensureUser_createsNewUserWithFallbackUsername_whenTelegramUsernameBlank() {
        long tgId = 12345L;
        when(userRepository.findByTgId(tgId)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.ensureUser(tgId, "   ");

        assertEquals(tgId, result.getTgId());
        assertEquals("Пользователь", result.getUsername());
        assertEquals(RegistrationStep.NONE, result.getRegistrationStep());
        assertFalse(result.isOnboarded());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUsernameAndComplete_setsOnboardedAndClearsStep() {
        long tgId = 999L;
        User existing = new User();
        existing.setTgId(tgId);
        existing.setUsername("Old");
        existing.setRegistrationStep(RegistrationStep.WAITING_USERNAME);
        existing.setOnboarded(false);

        when(userRepository.findByTgId(tgId)).thenReturn(Optional.of(existing));

        User updated = userService.updateUsernameAndComplete(tgId, "  New Name  ");

        assertEquals("New Name", updated.getUsername());
        assertTrue(updated.isOnboarded());
        assertEquals(RegistrationStep.NONE, updated.getRegistrationStep());
    }

    @Test
    void requireOnboardedUser_throwsForbidden_whenUserNotOnboarded() {
        long tgId = 777L;
        User existing = new User();
        existing.setTgId(tgId);
        existing.setOnboarded(false);

        when(userRepository.findByTgId(tgId)).thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> userService.requireOnboardedUser(tgId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}