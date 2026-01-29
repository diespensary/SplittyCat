package me.khromov.splittycat.api.app;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.service.CurrentUserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppInitController {

    private final CurrentUserService currentUserService;

    @GetMapping("/init")
    public InitResponse init() {
        var u = currentUserService.requireRegisteredUser();
        return new InitResponse(u.getId(), u.getUsername());
    }

    public record InitResponse(Long userId, String username) {}
}
