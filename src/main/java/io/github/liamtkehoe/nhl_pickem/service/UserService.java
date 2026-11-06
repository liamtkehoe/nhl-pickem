package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserService {
    private static final Pattern VALID_USERNAME = Pattern.compile("[A-Za-z0-9_-]{3,20}");

    private final UserRepository users;
    private final Clock clock;

    public UserService(UserRepository users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    public record LoginResult(User user, boolean created) {}

    //Finds the account or creates it the first time a username is used
    public LoginResult login(String rawUsername) {
        String username = rawUsername == null ? "" : rawUsername.trim();
        if (!VALID_USERNAME.matcher(username).matches()) {
            throw new InvalidRequestException(List.of("Usernames must be 3-20 letters, numbers, - or _"));
        }
        return users.findByUsername(username)
                .map(user -> new LoginResult(user, false))
                .orElseGet(() -> new LoginResult(users.create(username, clock.instant()), true));
    }

    public User require(String username) {
        return users.findByUsername(username.trim())
                .orElseThrow(() -> new NotFoundException("No player named " + username));
    }
}
