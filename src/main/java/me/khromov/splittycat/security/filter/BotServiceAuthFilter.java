package me.khromov.splittycat.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.security.config.SecurityProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class BotServiceAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bot ";
    private static final String TG_ID_HEADER = "X-Tg-Id";

    private final String serviceToken;

    public BotServiceAuthFilter(SecurityProperties props) {
        this.serviceToken = props.getBotService().getToken();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = AuthHeader.tokenWithPrefix(request, PREFIX);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!serviceToken.equals(token)) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            long tgId = Long.parseLong(AuthHeader.requiredHeader(request, TG_ID_HEADER));
            SecurityContextHolder.getContext().setAuthentication(UserAuthentication.botService(tgId));
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
