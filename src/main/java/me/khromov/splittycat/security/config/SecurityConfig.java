package me.khromov.splittycat.security.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import me.khromov.splittycat.security.filter.BotServiceAuthFilter;
import me.khromov.splittycat.security.filter.MiniAppAuthFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain botServiceChain(HttpSecurity http, SecurityProperties props) throws Exception {
        statelessApi(http);

        http.securityMatcher("/api/bot/**")
                .authorizeHttpRequests(a -> a.anyRequest().hasRole("BOT_SERVICE"))
                .addFilterBefore(new BotServiceAuthFilter(props), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain miniAppChain(
            HttpSecurity http,
            ObjectMapper objectMapper,
            SecurityProperties props
    ) throws Exception {

        statelessApi(http);

        http.securityMatcher("/api/app/**")
                .authorizeHttpRequests(a -> a.anyRequest().hasRole("MINI_APP"))
                .addFilterBefore(new MiniAppAuthFilter(objectMapper, props),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        statelessApi(http);

        http.securityMatcher("/actuator/**", "/error")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());

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
