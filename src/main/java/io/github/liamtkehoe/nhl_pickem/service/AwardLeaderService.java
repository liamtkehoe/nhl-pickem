package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.PlayerStats;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.repository.AwardResultsRepository;
import io.github.liamtkehoe.nhl_pickem.repository.OULinesRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import io.github.liamtkehoe.nhl_pickem.repository.StatsRepository;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardFormulas;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardLeader;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AwardLeaderService {
    private final StatsRepository stats;
    private final SnapshotRepository snapshots;
    private final OULinesRepository lines;
    private final AwardResultsRepository results;
    private final AwardFormulas formulas;
    private final Clock clock;
    private final int gamesInSeason;

    public AwardLeaderService(StatsRepository stats, SnapshotRepository snapshots, OULinesRepository lines, AwardResultsRepository results, AwardFormulas formulas, Clock clock, @Value("${pickem.games-per-season}") int gamesInSeason) {
        this.stats = stats;
        this.snapshots = snapshots;
        this.lines = lines;
        this.results = results;
        this.formulas = formulas;
        this.clock = clock;
        this.gamesInSeason = gamesInSeason;
    }

    public Map<Award, AwardLeader> currentLeaders(int seasonId) {
        Map<Award, AwardLeader> leaders = new EnumMap<>(Award.class);
        leaders.putAll(formulas.leaders(stats.find(seasonId), snapshots.teams(seasonId), lines.find(seasonId), gamesInSeason));
        leaders.putAll(results.finals(seasonId));

        return leaders;
    }

    public record WinnerRequest(Award award, Long playerId, String team) {}

    public void recordWinner(int seasonId, WinnerRequest request) {
        Award award = request.award();
        if (award == null) {
            throw new InvalidRequestException(List.of("award is required"));
        }
        if (award.isTeamPick()) {
            boolean known = snapshots.teams(seasonId).stream()
                    .map(TeamStanding::team)
                    .anyMatch(t -> t.equals(request.team()));
            if (!known) {
                throw new InvalidRequestException(List.of("Unknown team: " + request.team()));
            }
            results.saveFinal(seasonId, award, null, request.team(), "MANUAL", clock.instant());
        } else {
            boolean known = request.playerId() != null
                    && (stats.find(seasonId).stream().map(PlayerStats::playerId).anyMatch(request.playerId()::equals) || snapshots.players(seasonId).stream().map(PlayerInfo::playerId).anyMatch(request.playerId()::equals));
            if (!known) {
                throw new InvalidRequestException(List.of("Unknown player: " + request.playerId()));
            }
            results.saveFinal(seasonId, award, request.playerId(), null, "MANUAL", clock.instant());
        }
    }

    public List<AwardFormulas.Candidate> candidates(int seasonId, Award award, int limit) {
        return formulas.rankings(stats.find(seasonId), snapshots.teams(seasonId), lines.find(seasonId), gamesInSeason)
                .getOrDefault(award, List.of())
                .stream()
                .limit(limit)
                .toList();
    }
}
