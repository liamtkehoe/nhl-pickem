package io.github.liamtkehoe.nhl_pickem.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.AwardPick;
import io.github.liamtkehoe.nhl_pickem.model.OuPick;
import io.github.liamtkehoe.nhl_pickem.model.OverUnder;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.StandingsPick;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScoringEngineTest {
    private final ScoringEngine engine = new ScoringEngine();

    //Standings Tests
    @Test
    void standingsScoreTwoForExactAndOneForOffByOne() {
        SeasonState state = new SeasonState(
                List.of(team("PHI", 3, 14, 11), team("NJD", 1, 18, 12), team("NYR", 6, 9, 11)),
                Map.of(), Map.of(), 84);
        PickSet picks = new PickSet(List.of(
                new StandingsPick("PHI", 3),   // exact
                new StandingsPick("NJD", 2),   // off by one
                new StandingsPick("NYR", 3),   // off by three
                new StandingsPick("SEA", 1)),  // not in the standings
                List.of(), List.of());

        PlayerScore score = engine.score("tester", picks, state);

        assertThat(score.standings()).extracting(StandingsResult::points).containsExactly(2, 1, 0, 0);
        assertThat(score.standings().get(3).actualPlace()).isNull();
        assertThat(score.standingsPoints()).isEqualTo(3);
        assertThat(score.exactPlaces()).isEqualTo(1);
        assertThat(score.total()).isEqualTo(3);
    }

    //Over/Under Tests
    @Test
    void overIsLeadingWhenPaceBeatsTheLine() {
        OuResult result = overUnder(OverUnder.OVER, 14, 11, 95.5);   // pace 106.9
        assertThat(result.status()).isEqualTo(OuStatus.LEADING);
        assertThat(result.points()).isEqualTo(1);
        assertThat(result.projectedPoints()).isCloseTo(106.9, within(0.1));
    }

    @Test
    void underIsTrailingWhenPaceBeatsTheLine() {
        OuResult result = overUnder(OverUnder.UNDER, 14, 11, 95.5);
        assertThat(result.status()).isEqualTo(OuStatus.TRAILING);
        assertThat(result.points()).isZero();
    }

    @Test
    void overClinchesOncePointsPassTheLine() {
        assertThat(overUnder(OverUnder.OVER, 96, 70, 95.5).status()).isEqualTo(OuStatus.CLINCHED_WIN);
        assertThat(overUnder(OverUnder.UNDER, 96, 70, 95.5).status()).isEqualTo(OuStatus.CLINCHED_LOSS);
    }

    @Test
    void underClinchesWhenWinningOutCannotReachTheLine() {
        OuResult result = overUnder(OverUnder.UNDER, 60, 80, 95.5);   // max possible 68
        assertThat(result.status()).isEqualTo(OuStatus.CLINCHED_WIN);
        assertThat(result.points()).isEqualTo(1);
    }

    @Test
    void endOfSeasonUsesActualPoints() {
        OuResult result = overUnder(OverUnder.OVER, 99, 84, 99.5);
        assertThat(result.status()).isEqualTo(OuStatus.CLINCHED_LOSS);
        assertThat(result.points()).isZero();
    }

    @Test
    void landingExactlyOnAWholeNumberLineIsAPush() {
        OuResult result = overUnder(OverUnder.OVER, 96, 84, 96.0);
        assertThat(result.status()).isEqualTo(OuStatus.PUSH);
        assertThat(result.points()).isZero();
    }

    @Test
    void pendingBeforeFirstGameOrWithoutALine() {
        assertThat(overUnder(OverUnder.OVER, 0, 0, 95.5).status()).isEqualTo(OuStatus.PENDING);

        PickSet picks = new PickSet(List.of(), List.of(new OuPick("PHI", OverUnder.OVER)), List.of());
        SeasonState noLines = new SeasonState(List.of(team("PHI", 1, 14, 11)), Map.of(), Map.of(), 84);
        OuResult result = engine.score("tester", picks, noLines).overUnders().getFirst();

        assertThat(result.status()).isEqualTo(OuStatus.PENDING);
        assertThat(result.points()).isZero();
    }

    //Awards Tests
    @Test
    void awardsScoreThreeForMatchingTheLeader() {
        Map<Award, AwardLeader> leaders = Map.of(
                Award.ART_ROSS, new AwardLeader(Award.ART_ROSS, 100L, null, "Leader A", false),
                Award.HART, new AwardLeader(Award.HART, 6L, null, "Leader H", false),
                Award.JACK_ADAMS, new AwardLeader(Award.JACK_ADAMS, null, "PHI", "PHI head coach", false));
        PickSet picks = new PickSet(List.of(), List.of(), List.of(
                AwardPick.player(Award.ART_ROSS, 100, "Leader A"),            // match
                AwardPick.player(Award.HART, 5, "Someone Else"),              // wrong player
                AwardPick.team(Award.JACK_ADAMS, "PHI", "PHI head coach"),    // team match
                AwardPick.player(Award.VEZINA, 30, "A Goalie")));             // no leader yet

        PlayerScore score = engine.score("tester", picks, new SeasonState(List.of(), Map.of(), leaders, 84));

        assertThat(score.awards()).extracting(AwardResult::points).containsExactly(3, 0, 3, 0);
        assertThat(score.awards().get(3).leaderName()).isNull();
        assertThat(score.awardPoints()).isEqualTo(6);
    }

    //Leaderboard Tests
    @Test
    void leaderboardBreaksTiesThenSharesRanks() {
        PlayerScore a = summary("A", 50, 10, 20);
        PlayerScore b = summary("B", 50, 12, 15);
        PlayerScore c = summary("C", 50, 12, 15);   // tied with B on everything
        PlayerScore d = summary("D", 40, 15, 30);

        List<LeaderboardRow> rows = engine.leaderboard(List.of(a, d, c, b));

        assertThat(rows).extracting(row -> row.score().username()).containsExactly("B", "C", "A", "D");
        assertThat(rows).extracting(LeaderboardRow::rank).containsExactly(1, 1, 3, 4);
    }

    private static TeamStanding team(String abbrev, int place, int points, int gamesPlayed) {
        return new TeamStanding(abbrev, abbrev, "Metropolitan", place, points, gamesPlayed, Instant.EPOCH);
    }

    private OuResult overUnder(OverUnder pick, int points, int gamesPlayed, double line) {
        PickSet picks = new PickSet(List.of(), List.of(new OuPick("PHI", pick)), List.of());
        SeasonState state = new SeasonState(
                List.of(team("PHI", 1, points, gamesPlayed)), Map.of("PHI", line), Map.of(), 84);
        return engine.score("tester", picks, state).overUnders().getFirst();
    }

    private static PlayerScore summary(String name, int total, int exactPlaces, int correctOverUnders) {
        return new PlayerScore(name, total, 0, 0, 0, exactPlaces, correctOverUnders,
                List.of(), List.of(), List.of());
    }
}
