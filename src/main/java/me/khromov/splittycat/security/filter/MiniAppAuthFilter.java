package me.khromov.splittycat.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.security.config.SecurityProperties;
import me.khromov.splittycat.security.telegram.TelegramInitDataValidator;
import me.khromov.splittycat.security.telegram.TelegramUserPayload;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MiniAppAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "TMA ";

    private final TelegramInitDataValidator validator;

    public MiniAppAuthFilter(ObjectMapper objectMapper, SecurityProperties props) {
        this.validator = new TelegramInitDataValidator(
                objectMapper,
                props.getTelegram().getBotToken(),
                props.getTelegram().getInitDataMaxAgeSeconds()
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String initData = AuthHeader.tokenWithPrefix(request, PREFIX);
        if (initData == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TelegramUserPayload user = validator.validateAndExtractUser(initData);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(UserAuthentication.miniApp(user.id()));
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
