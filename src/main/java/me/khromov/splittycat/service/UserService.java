package me.khromov.splittycat.service;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.PendingAction;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String FALLBACK_USERNAME = "Пользователь";

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User requireRegisteredUser(long tgId) {
        return userRepository.findByTgId(tgId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Finish registration in the bot (/start) first."
                ));
    }

    @Transactional
    public User ensureUser(long tgId, String telegramUsername) {
        String candidate = normalize(telegramUsername);

        return userRepository.findByTgId(tgId)
                .map(u -> {
                    if (isBlank(u.getUsername())) {
                        u.setUsername(defaultUsername(candidate));
                    }
                    if (u.getPendingAction() == null) {
                        u.setPendingAction(PendingAction.NONE);
                    }
                    return u;
                })
                .orElseGet(() -> {
                    var u = new User();
                    u.setTgId(tgId);
                    u.setUsername(defaultUsername(candidate));
                    u.setPendingAction(PendingAction.NONE);
                    return userRepository.save(u);
                });
    }

    @Transactional
    public void startWaitingUsername(long tgId) {
        var u = requireRegisteredUser(tgId);
        u.setPendingAction(PendingAction.WAITING_USERNAME);
    }

    @Transactional
    public void clearPendingAction(long tgId) {
        var u = requireRegisteredUser(tgId);
        u.setPendingAction(PendingAction.NONE);
    }

    @Transactional(readOnly = true)
    public boolean isWaitingUsername(long tgId) {
        return userRepository.findByTgId(tgId)
                .map(u -> u.getPendingAction() == PendingAction.WAITING_USERNAME)
                .orElse(false);
    }

    @Transactional
    public User updateUsername(long tgId, String newUsername) {
        String username = normalize(newUsername);
        if (username.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username cannot be blank");
        }

        var u = requireRegisteredUser(tgId);
        u.setUsername(username);
        u.setPendingAction(PendingAction.NONE);
        return u;
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
