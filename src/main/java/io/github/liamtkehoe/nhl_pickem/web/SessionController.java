package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class SessionController {
    private final UserService users;
    private final PickRepository picks;
    private final int season;

    public SessionController(UserService users, PickRepository picks, @Value("${pickem.current-season}") int season) {
        this.users = users;
        this.picks = picks;
        this.season = season;
    }

    public record LoginRequest(String username) {}

    public record LoginResponse(long id, String username, boolean created, Instant submittedAt) {}

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        UserService.LoginResult result = users.login(request.username());
        User user = result.user();

        return new LoginResponse(user.id(), user.username(), result.created(), picks.submittedAt(user.id(), season).orElse(null));
    }
}
