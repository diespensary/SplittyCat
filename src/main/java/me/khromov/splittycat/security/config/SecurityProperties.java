package me.khromov.splittycat.security.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    @Valid
    private Telegram telegram = new Telegram();

    @Valid
    private BotService botService = new BotService();

    @Getter
    @Setter
    public static class Telegram {
        @NotBlank
        private String botToken;

        @Min(1)
        private long initDataMaxAgeSeconds;
    }

    @Getter
    @Setter
    public static class BotService {
        @NotBlank
        private String token;
    }
}

