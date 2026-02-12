package me.khromov.splittycat.security.auth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class UserAuthentication extends AbstractAuthenticationToken {

    private final UserPrincipal principal;

    private UserAuthentication(long tgId, boolean isAdmin) {
        super(
                isAdmin
                        ? List.of(
                        new SimpleGrantedAuthority(AuthRole.ROLE_USER.name()),
                        new SimpleGrantedAuthority(AuthRole.ROLE_ADMIN.name())
                )
                        : List.of(new SimpleGrantedAuthority(AuthRole.ROLE_USER.name()))
        );
        this.principal = new UserPrincipal(tgId);
        setAuthenticated(true);
    }

    public static UserAuthentication user(long tgId) {
        return new UserAuthentication(tgId, false);
    }

    public static UserAuthentication admin(long tgId) {
        return new UserAuthentication(tgId, true);
    }

    @Override public Object getCredentials() { return null; }
    @Override public UserPrincipal getPrincipal() { return principal; }
    @Override public String getName() { return String.valueOf(principal.tgId()); }
}