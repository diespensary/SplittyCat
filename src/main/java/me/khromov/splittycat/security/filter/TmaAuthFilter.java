package me.khromov.splittycat.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.security.util.AuthHeader;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.telegram.validation.TelegramInitDataValidator;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TmaAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "TMA ";

    private final TelegramInitDataValidator validator;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String initData = AuthHeader.tokenWithPrefix(request, PREFIX);
        if (initData == null) {
            initData = request.getHeader("X-TMA-Init-Data");
        }

        if (initData == null || initData.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var user = validator.validateAndExtractUser(initData);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                SecurityContextHolder.getContext().setAuthentication(UserAuthentication.user(user.id()));
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
