package me.khromov.splittycat.api.bot;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotRegisterController {

    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    public void register(@RequestBody RegisterRequest request) {
        currentUserService.registerOrUpdate(request.username());
    }

    public record RegisterRequest(String username) {}
}
