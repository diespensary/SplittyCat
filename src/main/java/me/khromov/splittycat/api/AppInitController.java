package me.khromov.splittycat.api;

import lombok.RequiredArgsConstructor;
import me.khromov.splittycat.api.dto.InitResponse;
import me.khromov.splittycat.security.CurrentUser;
import me.khromov.splittycat.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppInitController {

    private final CurrentUser currentUser;
    private final UserService userService;

    @GetMapping("/init")
    public InitResponse init() {
        var u = userService.requireOnboardedUser(currentUser.tgId());
        return new InitResponse(u.getId(), u.getUsername());
    }

}
