package me.khromov.splittycat.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.security.util.AuthHeader;
import me.khromov.splittycat.security.auth.UserAuthentication;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import me.khromov.splittycat.telegram.dto.TelegramUserPayload;
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

    private final TelegramInitDataValidator validator;
    private final TelegramProperties telegramProperties;

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

        logger.debug("Received initData: {}", initData);

        if (initData == null || initData.isBlank()) {
            logger.warn("No initData found, passing request through.");
            filterChain.doFilter(request, response);
            return;
        }

        try {
            TelegramUserPayload user = validator.validateAndExtractUser(initData);
            logger.debug("User validated: {}", user);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                List<Long> adminIds = telegramProperties.getAdminIds();
                boolean isAdmin = adminIds != null && adminIds.contains(user.id());
                SecurityContextHolder.getContext().setAuthentication(
                        isAdmin
                                ? UserAuthentication.admin(user.id())
                                : UserAuthentication.user(user.id())
                );
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("Error during validation or authentication", e);
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}