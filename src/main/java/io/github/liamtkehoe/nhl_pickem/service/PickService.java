package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PickService {
    private final SeasonRepository seasons;
    private final PickRepository picks;
    private final SnapshotRepository snapshots;
    private final PickValidator validator;
    private final Clock clock;

    public PickService(SeasonRepository seasons, PickRepository picks, SnapshotRepository snapshots, PickValidator validator, Clock clock) {
        this.seasons = seasons;
        this.picks = picks;
        this.snapshots = snapshots;
        this.validator = validator;
        this.clock = clock;
    }

    //Validates a submission from the API against the live standings and rosters, then saves it
    @Transactional
    public Instant submit(long userId, int seasonId, SubmitPicksRequest request) {
        Instant now = checkCanSubmit(userId, seasonId);
        Map<Long, PlayerInfo> players = snapshots.players(seasonId).stream()
                .collect(Collectors.toMap(PlayerInfo::playerId, Function.identity()));
        PickSet pickSet = validator.validate(request, snapshots.teams(seasonId), players);
        picks.insertAll(userId, seasonId, pickSet, now);

        return now;
    }

    //Saves an already built pick set
    @Transactional
    public Instant submit(long userId, int seasonId, PickSet pickSet) {
        Instant now = checkCanSubmit(userId, seasonId);
        picks.insertAll(userId, seasonId, pickSet, now);

        return now;
    }

    private Instant checkCanSubmit(long userId, int seasonId) {
        Season season = seasons.find(seasonId)
                .orElseThrow(() -> new PickException("Unknown season " + seasonId));
        Instant now = clock.instant();
        if (!season.isOpen(now)) {
            throw new PickException("Picks are closed for season " + seasonId);
        }
        if (picks.submittedAt(userId, seasonId).isPresent()) {
            throw new PickException("Picks already submitted for season " + seasonId + ". No changing them now!");
        }

        return now;
    }
}
