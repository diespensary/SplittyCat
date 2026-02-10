package me.khromov.splittycat.security.config;

import me.khromov.splittycat.security.filter.TmaAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

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
                                response.sendError(401))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(403)))
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
