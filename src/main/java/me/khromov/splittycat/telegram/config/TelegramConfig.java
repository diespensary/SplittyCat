package me.khromov.splittycat.telegram.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.khromov.splittycat.telegram.validation.TelegramInitDataValidator;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    @Bean
    public TelegramInitDataValidator telegramInitDataValidator(TelegramProperties props) {
        var mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return new TelegramInitDataValidator(mapper, props.getBotToken(), props.getInitDataMaxAgeSeconds());
    }

}
