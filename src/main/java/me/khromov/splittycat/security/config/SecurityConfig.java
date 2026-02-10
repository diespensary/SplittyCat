package me.khromov.splittycat.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.khromov.splittycat.api.dto.ApiErrorResponse;
import me.khromov.splittycat.security.filter.TmaAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.time.Instant;

@Configuration
public class SecurityConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static void writeApiError(
            jakarta.servlet.http.HttpServletResponse response,
            int statusCode,
            String message,
            String path
    ) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");

        org.springframework.http.HttpStatus status = org.springframework.http.HttpStatus.valueOf(statusCode);
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                statusCode,
                status.getReasonPhrase(),
                message,
                path
        );

        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }

    @Bean
    @Order(0)
    public SecurityFilterChain botWebhookChain(HttpSecurity http) throws Exception {
        statelessApi(http);

        http.securityMatcher("/bot/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http, TmaAuthFilter tmaAuthFilter) throws Exception {
        statelessApi(http);

        http.securityMatcher("/api/**")
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) ->
                                writeApiError(response, 401, "Требуется авторизация в Telegram Mini App", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeApiError(response, 403, "Недостаточно прав для этого действия", request.getRequestURI())))
                .addFilterBefore(tmaAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        statelessApi(http);

        http.securityMatcher(
                "/",
                "/index.html",
                "/*.js",
                "/*.css",
                "/*.png",
                "/*.svg",
                "/assets/**",
                "/actuator/**",
                "/error"
        ).authorizeHttpRequests(a -> a.anyRequest().permitAll());


        return http.build();
    }

    @Bean
    @Order(99)
    public SecurityFilterChain denyAllChain(HttpSecurity http) throws Exception {
        statelessApi(http);
        http.authorizeHttpRequests(a -> a.anyRequest().denyAll());
        return http.build();
    }

    private static void statelessApi(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(hb -> hb.disable());
    }
}
