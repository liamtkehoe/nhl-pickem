package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class PickService {
    private final SeasonRepository seasons;
    private final PickRepository picks;
    private final Clock clock;

    public PickService(SeasonRepository seasons, PickRepository picks, Clock clock) {
        this.seasons = seasons;
        this.picks = picks;
        this.clock = clock;
    }

    /** Saves a player's picks for a season, all or nothing. Returns the submission time */
    @Transactional
    public Instant submit(long userId, int seasonId, PickSet pickSet) {
        Season season = seasons.find(seasonId)
                .orElseThrow(() -> new PickException("Unknown season " + seasonId));
        Instant now = clock.instant();

        if (!season.isOpen(now)) {
            throw new PickException("Picks are closed for season " + seasonId);
        }
        if (picks.submittedAt(userId, seasonId).isPresent()) {
            throw new PickException("Picks already submitted for season " + seasonId + ". No changing them now!");
        }

        picks.insertAll(userId, seasonId, pickSet, now);
        return now;
    }
}
