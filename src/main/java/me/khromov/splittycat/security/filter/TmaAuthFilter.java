package me.khromov.splittycat.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.security.util.AuthHeader;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import me.khromov.splittycat.telegram.dto.TelegramUserPayload;
import me.khromov.splittycat.telegram.validation.TelegramInitDataValidationException;
import me.khromov.splittycat.telegram.validation.TelegramInitDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TmaAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(TmaAuthFilter.class);
    private static final String PREFIX = "TMA ";
    private static final String INIT_DATA_HEADER = "X-TMA-Init-Data";

    private final TelegramInitDataValidator validator;
    private final TelegramProperties telegramProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String initData = resolveInitData(request);
        if (initData == null || initData.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            authenticate(validator.validateAndExtractUser(initData));
            filterChain.doFilter(request, response);
        } catch (TelegramInitDataValidationException exception) {
            logger.warn("Telegram Mini App authentication failed: {}", exception.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private String resolveInitData(HttpServletRequest request) {
        String initData = AuthHeader.tokenWithPrefix(request, PREFIX);
        return initData != null ? initData : request.getHeader(INIT_DATA_HEADER);
    }

    private void authenticate(TelegramUserPayload user) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        List<Long> adminIds = telegramProperties.getAdminIds();
        boolean isAdmin = adminIds != null && adminIds.contains(user.id());

        SecurityContextHolder.getContext().setAuthentication(
                isAdmin ? UserAuthentication.admin(user.id()) : UserAuthentication.user(user.id())
        );
    }
}
