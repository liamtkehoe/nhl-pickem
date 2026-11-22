package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import io.github.liamtkehoe.nhl_pickem.service.NotFoundException;
import java.time.Clock;
import java.time.Instant;

import io.github.liamtkehoe.nhl_pickem.service.ScoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SeasonController {
    private final SeasonRepository seasons;
    private final Clock clock;
    private final int season;
    private final ScoreService scores;

    public SeasonController(SeasonRepository seasons, Clock clock, ScoreService scores, @Value("${pickem.current-season}") int season) {
        this.seasons = seasons;
        this.clock = clock;
        this.season = season;
        this.scores = scores;
    }

    public record SeasonInfo(int id, String label, boolean practice, boolean open, Instant picksCloseAt, boolean finalized) {}

    @GetMapping("/season")
    public SeasonInfo current() {
        Season s = seasons.find(season)
                .orElseThrow(() -> new NotFoundException("Season " + season + " isn't set up yet"));

        return new SeasonInfo(s.id(), s.label(), s.practice(), s.isOpen(clock.instant()), s.picksCloseAt(), scores.seasonFinalized());
    }
}
