package me.khromov.splittycat.api;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.domain.entity.User;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.service.UserAccountService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApiUserProvider {

    private final CurrentUser currentUser;
    private final UserAccountService userAccountService;

    public User getCurrentOnboardedUser() {
        return userAccountService.requireOnboardedUser(currentUser.tgId());
    }
}
