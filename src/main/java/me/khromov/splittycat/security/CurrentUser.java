package me.khromov.splittycat.security;

import me.khromov.splittycat.security.auth.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUser {

    public long tgId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return p.tgId();
    }
}
