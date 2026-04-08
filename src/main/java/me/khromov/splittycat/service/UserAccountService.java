package me.khromov.splittycat.service;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.common.exception.ForbiddenException;
import me.khromov.splittycat.common.util.InputNormalizer;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAccountService {

    private static final String FALLBACK_USERNAME = "Пользователь";
    private static final String REGISTRATION_REQUIRED_MESSAGE = "Сначала завершите регистрацию в боте (/start)";

    private final UserRepository userRepository;

    @Transactional
    public User ensureUser(long tgId, String telegramUsername) {
        String normalizedUsername = InputNormalizer.trimOrEmpty(telegramUsername);

        return userRepository.findByTgId(tgId)
                .map(user -> initializeExistingUser(user, normalizedUsername))
                .orElseGet(() -> userRepository.save(User.create(tgId, defaultUsername(normalizedUsername))));
    }

    public User requireRegisteredUser(long tgId) {
        return userRepository.findByTgId(tgId)
                .orElseThrow(() -> new ForbiddenException(REGISTRATION_REQUIRED_MESSAGE));
    }

    public User requireOnboardedUser(long tgId) {
        User user = requireRegisteredUser(tgId);
        if (!user.isOnboarded()) {
            throw new ForbiddenException(REGISTRATION_REQUIRED_MESSAGE);
        }
        return user;
    }

    private User initializeExistingUser(User user, String normalizedUsername) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            user.applyUsername(defaultUsername(normalizedUsername));
        }
        user.ensureRegistrationStep();
        return user;
    }

    private static String defaultUsername(String candidate) {
        return candidate.isBlank() ? FALLBACK_USERNAME : candidate;
    }
}
