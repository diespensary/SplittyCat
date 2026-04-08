package me.khromov.splittycat.security;

import me.khromov.splittycat.common.exception.UnauthorizedException;
import me.khromov.splittycat.security.auth.UserPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public long tgId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Требуется авторизация");
        }
        return principal.tgId();
    }
}
