package io.github.liamtkehoe.nhl_pickem.web;

import static io.github.liamtkehoe.nhl_pickem.web.PicksController.USER_HEADER;

import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.scoring.PlayerScore;
import io.github.liamtkehoe.nhl_pickem.service.ForbiddenException;
import io.github.liamtkehoe.nhl_pickem.service.LeaderboardEntry;
import io.github.liamtkehoe.nhl_pickem.service.NotFoundException;
import io.github.liamtkehoe.nhl_pickem.service.ScoreService;
import io.github.liamtkehoe.nhl_pickem.service.UserService;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScoresController {
    private final UserService users;
    private final ScoreService scores;
    private final PickRepository picks;
    private final int season;

    public ScoresController(UserService users, ScoreService scores, PickRepository picks, @Value("${pickem.current-season}") int season) {
        this.users = users;
        this.scores = scores;
        this.picks = picks;
        this.season = season;
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard(@RequestHeader(USER_HEADER) String username) {
        users.require(username);

        return scores.leaderboard();
    }

    @GetMapping("/scores/{target}")
    public PlayerScore score(@RequestHeader(USER_HEADER) String username, @PathVariable String target) {
        User viewer = users.require(username);
        User subject = users.require(target);

        boolean ownPicks = viewer.id() == subject.id();
        if (!ownPicks && picks.submittedAt(viewer.id(), season).isEmpty()) {
            throw new ForbiddenException("Submit your own picks to see other players' picks");
        }

        return scores.score(subject)
                .orElseThrow(() -> new NotFoundException(subject.username() + " hasn't submitted picks yet"));
    }
}
