package me.khromov.splittycat.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class UserAuthentication extends AbstractAuthenticationToken {

    private final UserPrincipal principal;

    private UserAuthentication(long tgId) {
        super(List.of(new SimpleGrantedAuthority(AuthRole.ROLE_USER.name())));
        this.principal = new UserPrincipal(tgId);
        setAuthenticated(true);
    }

    public static UserAuthentication user(long tgId) {
        return new UserAuthentication(tgId);
    }

    @Override public Object getCredentials() { return null; }
    @Override public UserPrincipal getPrincipal() { return principal; }
    @Override public String getName() { return String.valueOf(principal.tgId()); }
}
