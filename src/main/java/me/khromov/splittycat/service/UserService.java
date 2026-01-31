package me.khromov.splittycat.service;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_USERNAME = "Пользователь";

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
    public User registerOrUpdate(long tgId, String desiredUsername) {
        String username = normalize(desiredUsername);

        return userRepository.findByTgId(tgId)
                .map(u -> {
                    if (!username.isBlank() && !username.equals(u.getUsername())) {
                        u.setUsername(username);
                    }
                    if (u.getUsername() == null || u.getUsername().isBlank()) {
                        u.setUsername(DEFAULT_USERNAME);
                    }
                    return u;
                })
                .orElseGet(() -> {
                    var u = new User();
                    u.setTgId(tgId);
                    u.setUsername(username.isBlank() ? DEFAULT_USERNAME : username);
                    return userRepository.save(u);
                });
    }

    @Transactional(readOnly = true)
    public boolean isRegistered(long tgId) {
        return userRepository.findByTgId(tgId).isPresent();
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
