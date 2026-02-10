package me.khromov.splittycat.telegram.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.telegram")
public class TelegramProperties {

    @NotBlank
    private String botToken;

    @Min(1)
    private long initDataMaxAgeSeconds;

    private String webhookUrl;
    private String webhookSecret;
    private String botUsername;
}

