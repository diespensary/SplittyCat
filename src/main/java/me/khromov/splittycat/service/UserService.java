package me.khromov.splittycat.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.RegistrationStep;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final String FALLBACK_USERNAME = "Пользователь";

    private static final List<RegistrationStep> REGISTRATION_FLOW =
            List.of(RegistrationStep.USERNAME_CHOICE, RegistrationStep.WAITING_USERNAME);

    private final UserRepository userRepository;

    @Transactional
    public User ensureUser(long tgId, String telegramUsername) {
        String candidate = normalize(telegramUsername);

        return userRepository.findByTgId(tgId)
                .map(u -> {
                    if (isBlank(u.getUsername())) {
                        u.setUsername(defaultUsername(candidate));
                    }
                    if (u.getRegistrationStep() == null) u.setRegistrationStep(RegistrationStep.NONE);
                    return u;
                })
                .orElseGet(() -> {
                    var u = new User();
                    u.setTgId(tgId);
                    u.setUsername(defaultUsername(candidate));
                    u.setRegistrationStep(RegistrationStep.NONE);
                    u.setOnboarded(false);
                    return userRepository.save(u);
                });
    }

    @Transactional
    public User requireOnboardedUser(long tgId) {
        var u = userRepository.findByTgId(tgId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Finish registration in the bot (/start) first.")
        );
        if (!u.isOnboarded()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Finish registration in the bot (/start) first.");
        }
        return u;
    }

    @Transactional
    public void startRegistration(long tgId) {
        var u = requireRegisteredUser(tgId);
        u.setOnboarded(false);
        u.setRegistrationStep(REGISTRATION_FLOW.get(0));
    }

    @Transactional
    public void completeRegistration(long tgId) {
        var u = requireRegisteredUser(tgId);
        u.setOnboarded(true);
        u.setRegistrationStep(RegistrationStep.NONE);
    }

    @Transactional
    public RegistrationStep getCurrentStep(long tgId) {
        return userRepository.findByTgId(tgId)
                .map(User::getRegistrationStep)
                .orElse(RegistrationStep.NONE);
    }

    @Transactional
    public void proceedToNextStep(long tgId) {
        var u = requireRegisteredUser(tgId);
        int idx = REGISTRATION_FLOW.indexOf(u.getRegistrationStep());
        if (idx >= 0 && idx + 1 < REGISTRATION_FLOW.size()) {
            u.setRegistrationStep(REGISTRATION_FLOW.get(idx + 1));
        } else {
            u.setRegistrationStep(RegistrationStep.NONE);
        }
    }

    @Transactional
    public User updateUsernameAndComplete(long tgId, String newUsername) {
        String username = normalize(newUsername);
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be blank");
        }
        var u = requireRegisteredUser(tgId);
        u.setUsername(username);
        u.setOnboarded(true);
        u.setRegistrationStep(RegistrationStep.NONE);
        return u;
    }

    @Transactional
    public User requireRegisteredUser(long tgId) {
        return userRepository.findByTgId(tgId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Finish registration in the bot (/start) first."));
    }

    private static String defaultUsername(String candidate) {
        return candidate.isBlank() ? FALLBACK_USERNAME : candidate;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
