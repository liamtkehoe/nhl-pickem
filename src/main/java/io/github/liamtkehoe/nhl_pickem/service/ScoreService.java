package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.Submission;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.OULinesRepository;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import io.github.liamtkehoe.nhl_pickem.scoring.PlayerScore;
import io.github.liamtkehoe.nhl_pickem.scoring.ScoringEngine;
import io.github.liamtkehoe.nhl_pickem.scoring.SeasonState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ScoreService {
    private final SnapshotRepository snapshots;
    private final OULinesRepository lines;
    private final PickRepository picks;
    private final AwardLeaderService awardLeaders;
    private final ScoringEngine engine;
    private final int season;
    private final int gamesInSeason;
    private final boolean previewSummary;

    public ScoreService(SnapshotRepository snapshots, OULinesRepository lines, PickRepository picks, AwardLeaderService awardLeaders, ScoringEngine engine, @Value("${pickem.current-season}") int season, @Value("${pickem.games-per-season}") int gamesInSeason, @Value("${pickem.preview-season-summary:false}") boolean previewSummary) {
        this.snapshots = snapshots;
        this.lines = lines;
        this.picks = picks;
        this.awardLeaders = awardLeaders;
        this.engine = engine;
        this.season = season;
        this.gamesInSeason = gamesInSeason;
        this.previewSummary = previewSummary;
    }

    public SeasonState seasonState() {
        return new SeasonState(snapshots.teams(season), lines.find(season), awardLeaders.currentLeaders(season), gamesInSeason);
    }

    public List<LeaderboardEntry> leaderboard() {
        SeasonState state = seasonState();
        List<Submission> submissions = picks.submissions(season);
        Map<String, Instant> submittedAt = submissions.stream()
                .collect(Collectors.toMap(Submission::username, Submission::submittedAt));

        List<PlayerScore> scores = submissions.stream()
                .map(s -> engine.score(s.username(), picks.find(s.userId(), season), state))
                .toList();

        return engine.leaderboard(scores).stream()
                .map(row -> {
                    PlayerScore s = row.score();
                    return new LeaderboardEntry(row.rank(), s.username(), s.total(), s.standingsPoints(), s.overUnderPoints(), s.awardPoints(), s.exactPlaces(), s.correctOverUnders(), submittedAt.get(s.username()));
                })
                .toList();
    }

    //Empty if the player hasn't submitted for this season
    public Optional<PlayerScore> score(User user) {
        if (picks.submittedAt(user.id(), season).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(engine.score(user.username(), picks.find(user.id(), season), seasonState()));
    }

    //True once every team has played out the season and every award has a final winner
    public boolean seasonFinalized() {
        if (previewSummary) {
            return true;
        }
        SeasonState state = seasonState();
        boolean everyGamePlayed = !state.standings().isEmpty()
                && state.standings().stream().allMatch(t -> t.gamesPlayed() >= gamesInSeason);
        boolean everyAwardFinal = Arrays.stream(Award.values())
                .allMatch(award -> state.leaders().containsKey(award) && state.leaders().get(award).isFinal());

        return everyGamePlayed && everyAwardFinal;
    }
}
