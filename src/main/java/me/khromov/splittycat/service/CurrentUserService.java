package me.khromov.splittycat.service;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.entity.User;
import me.khromov.splittycat.repository.UserRepository;
import me.khromov.splittycat.security.auth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private static final String DEFAULT_USERNAME = "Пользователь";

    private final UserRepository userRepository;

    public long requireTgId() {
        return requirePrincipal().tgId();
    }

    private static UserPrincipal requirePrincipal() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return p;
    }

    @Transactional(readOnly = true)
    public User requireRegisteredUser() {
        long tgId = requireTgId();
        return userRepository.findByTgId(tgId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Finish registration in the bot (/start) first."
                ));
    }

    @Transactional
    public User registerOrUpdate(String desiredUsername) {
        long tgId = requireTgId();
        String desired = normalize(desiredUsername);

        return userRepository.findByTgId(tgId)
                .map(u -> {
                    if (!desired.isBlank() && !desired.equals(u.getUsername())) {
                        u.setUsername(desired);
                    }
                    if (u.getUsername() == null || u.getUsername().isBlank()) {
                        u.setUsername(DEFAULT_USERNAME);
                    }
                    return u;
                })
                .orElseGet(() -> {
                    User u = new User();
                    u.setTgId(tgId);
                    u.setUsername(desired.isBlank() ? DEFAULT_USERNAME : desired);
                    return userRepository.save(u);
                });
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim();
    }
}
