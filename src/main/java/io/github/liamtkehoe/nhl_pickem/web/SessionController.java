package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.service.AdminAccess;
import io.github.liamtkehoe.nhl_pickem.service.UserService;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SessionController {
    private final UserService users;
    private final PickRepository picks;
    private final int season;
    private final AdminAccess admins;

    public SessionController(UserService users, PickRepository picks, AdminAccess admins, @Value("${pickem.current-season}") int season) {
        this.users = users;
        this.picks = picks;
        this.season = season;
        this.admins = admins;
    }

    public record LoginRequest(String username) {}

    public record LoginResponse(long id, String username, boolean created, Instant submittedAt, boolean admin) {}

    @PostMapping("/signup")
    public LoginResponse signUp(@RequestBody LoginRequest request) {
        return response(users.signUp(request.username()), true);
    }

    @PostMapping("/login")
    public LoginResponse logIn(@RequestBody LoginRequest request) {
        return response(users.logIn(request.username()), false);
    }

    private LoginResponse response(User user, boolean created) {
        return new LoginResponse(user.id(), user.username(), created, picks.submittedAt(user.id(), season).orElse(null), admins.isAdmin(user.username()));
    }
}