package me.khromov.splittycat.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class UserAuthentication extends AbstractAuthenticationToken {

    private final UserPrincipal principal;

    private UserAuthentication(long tgId, String authority) {
        super(List.of(new SimpleGrantedAuthority(authority)));
        this.principal = new UserPrincipal(tgId);
        setAuthenticated(true);
    }

    public static UserAuthentication miniApp(long tgId) {
        return new UserAuthentication(tgId, AuthRole.ROLE_MINI_APP.name());
    }

    public static UserAuthentication botService(long tgId) {
        return new UserAuthentication(tgId, AuthRole.ROLE_BOT_SERVICE.name());
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return String.valueOf(principal.tgId());
    }
}
