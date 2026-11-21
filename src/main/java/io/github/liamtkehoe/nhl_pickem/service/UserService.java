package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.UserRepository;
import org.springframework.dao.DataAccessException;
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

    //Creates a new account. Refuses a username that's taken in any capitalization
    public User signUp(String rawUsername) {
        String username = clean(rawUsername);
        if (!VALID_USERNAME.matcher(username).matches()) {
            throw new InvalidRequestException(List.of("Usernames must be 3-20 letters, numbers, - or _"));
        }
        if (users.findByUsername(username).isPresent()) {
            throw new ConflictException(taken(username));
        }
        try {
            return users.create(username, clock.instant());
        } catch (DataAccessException e) {
            throw new ConflictException(taken(username));
        }
    }

    //Logs in to an existing account. Never creates a new one
    public User logIn(String rawUsername) {
        String username = clean(rawUsername);
        if (username.isEmpty()) {
            throw new InvalidRequestException(List.of("Enter your username"));
        }
        return users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException(
                        "No account named \"" + username + "\". Please check the spelling, or sign up instead."));
    }

    private static String clean(String raw) {
        return raw == null ? "" : raw.trim();
    }

    private static String taken(String username) {
        return "The username \"" + username + "\" is already taken. If it is yours, please log in instead.";
    }

    public User require(String username) {
        return users.findByUsername(username.trim())
                .orElseThrow(() -> new NotFoundException("No player named " + username));
    }
}
