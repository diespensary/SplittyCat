package me.khromov.splittycat.api;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.InitResponse;
import me.khromov.splittycat.telegram.config.TelegramProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppInitController {

    private final ApiUserProvider apiUserProvider;
    private final TelegramProperties telegramProperties;

    @GetMapping("/init")
    public InitResponse init() {
        var user = apiUserProvider.getCurrentOnboardedUser();
        return new InitResponse(user.getId(), user.getUsername(), telegramProperties.getBotUsername());
    }
}
