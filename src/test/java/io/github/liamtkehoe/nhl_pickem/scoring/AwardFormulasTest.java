package io.github.liamtkehoe.nhl_pickem.scoring;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PlayerStats;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AwardFormulasTest {
    private final AwardFormulas formulas = new AwardFormulas(AwardWeights.defaults());

    private static final List<PlayerStats> LEAGUE = List.of(
            skater(1, "Alpha Center",   "C", "AAA", 20, 30, 30,  12, 10, 20,  5,   30,  0, false),
            skater(2, "Beta Wing",      "L", "BBB", 20,  6,  8,   6,  4, 14, 25,  120,  2, true),
            skater(3, "Gamma Wing",     "R", "BBB", 20, 10, 10,  -4, 12,  5,  8,   95,  6, true),
            skater(4, "Delta Defense",  "D", "AAA", 20,  5, 20,  15,  6, 10, 40,  150,  3, false),
            skater(5, "Echo Defense",   "D", "BBB", 20,  8, 25,  -6, 15,  4, 30,   60,  2, false),
            skater(6, "Fox Callup",     "C", "AAA",  3,  4,  4,  20,  0, 15, 10,  160,  0, true),
            skater(10, "Grinder",       "C", "AAA", 20,  2,  3,  10,  2, 20, 40,  180,  0, false),
            goalie(7, "Goalie One",  "AAA", 18, 14, 2, 0.930, 2.10, false),
            goalie(8, "Goalie Two",  "BBB", 17,  8, 0, 0.900, 3.00, false),
            goalie(9, "Hot Backup",  "AAA",  3,  3, 1, 1.000, 0.00, true)
    );

    private static final List<PlayerStats> CALDER_LEAGUE = List.of(
            skater(11, "Rookie Defenseman", "D", "AAA", 20,  3, 12, 0, 0, 0, 0, 0, 0, true),
            skater(12, "Rookie Sniper",     "C", "BBB", 20,  8,  4, 0, 0, 0, 0, 0, 0, true),
            skater(13, "Veteran Star",      "C", "AAA", 20, 12, 12, 0, 0, 0, 0, 0, 0, false),
            skater(14, "Veteran Wing",      "L", "BBB", 20,  4,  6, 0, 0, 0, 0, 0, 0, false),
            skater(15, "Veteran Defenseman","D", "BBB", 20,  1,  5, 0, 0, 0, 0, 0, 0, false),
            goalie(16, "Rookie Goalie",   "AAA", 15, 10, 2, 0.925, 2.20, true),
            goalie(17, "Veteran Goalie",  "BBB", 15,  9, 0, 0.905, 2.80, false),
            goalie(18, "Other Goalie",    "AAA", 15,  7, 0, 0.895, 3.10, false));

    private static final List<TeamStanding> TEAMS = List.of(
            team("AAA", "Alpha Club", 30, 20),
            team("BBB", "Beta Club", 20, 20));

    private static final Map<String, Double> LINES = Map.of("AAA", 100.0, "BBB", 70.0);

    private Map<Award, AwardLeader> leaders() {
        return formulas.leaders(LEAGUE, TEAMS, LINES, 84);
    }

    @Test
    void artRossAndRocketRichardGoToTheStatLeader() {
        assertThat(leaders().get(Award.ART_ROSS).playerId()).isEqualTo(1L);
        assertThat(leaders().get(Award.ROCKET_RICHARD).playerId()).isEqualTo(1L);
    }

    @Test
    void artRossTieGoesToMoreGoals() {
        List<PlayerStats> tied = List.of(
                skater(1, "Playmaker", "C", "AAA", 20, 10, 20, 0, 0, 0, 0, 0, 0, false),
                skater(2, "Sniper",    "L", "AAA", 20, 15, 15, 0, 0, 0, 0, 0, 0, false));

        assertThat(formulas.leaders(tied, TEAMS, LINES, 84).get(Award.ART_ROSS).playerId()).isEqualTo(2L);
    }

    @Test
    void hartFavorsADominantSkaterOverAGoodGoalie() {
        assertThat(leaders().get(Award.HART).playerId()).isEqualTo(1L);
    }

    @Test
    void calderRanksAHigherScoringDefensemanAboveAGoalScoringForward() {
        List<Long> ranking = formulas.rankings(CALDER_LEAGUE, TEAMS, LINES, 84).get(Award.CALDER).stream()
                .map(AwardFormulas.Candidate::playerId)
                .toList();

        assertThat(ranking.indexOf(11L)).isLessThan(ranking.indexOf(12L));
    }

    @Test
    void calderGoaliePenaltyDecidesBetweenARookieGoalieAndTheBestRookieSkater() {
        AwardFormulas strict = new AwardFormulas(withCalderGoaliePenalty(5.0));
        AwardFormulas lenient = new AwardFormulas(withCalderGoaliePenalty(0.0));

        assertThat(strict.leaders(CALDER_LEAGUE, TEAMS, LINES, 84).get(Award.CALDER).playerId()).isEqualTo(11L);
        assertThat(lenient.leaders(CALDER_LEAGUE, TEAMS, LINES, 84).get(Award.CALDER).playerId()).isEqualTo(16L);
    }

    @Test
    void vezinaIgnoresGoaliesBelowHalfTheirTeamsGames() {
        assertThat(leaders().get(Award.VEZINA).playerId()).isEqualTo(7L);
    }

    @Test
    void norrisOnlyConsidersDefensemenAndWeightsProductionMost() {
        assertThat(leaders().get(Award.NORRIS).playerId()).isEqualTo(5L);
    }

    @Test
    void selkeOnlyRanksTrustedPenaltyKillers() {
        List<Long> ranking = formulas.rankings(LEAGUE, TEAMS, LINES, 84).get(Award.SELKE).stream()
                .map(AwardFormulas.Candidate::playerId)
                .toList();

        assertThat(ranking).containsExactly(2L, 3L);
    }

    @Test
    void jackAdamsGoesToTheTeamFurthestAheadOfItsLine() {
        AwardLeader leader = leaders().get(Award.JACK_ADAMS);
        assertThat(leader.team()).isEqualTo("AAA");
        assertThat(leader.name()).isEqualTo("Alpha Club");
    }

    @Test
    void votedAwardsAreAlwaysProjected() {
        assertThat(leaders().get(Award.HART).isFinal()).isFalse();
        assertThat(leaders().get(Award.ART_ROSS).isFinal()).isFalse();
    }

    @Test
    void statAwardsBecomeFinalWhenEveryTeamHasPlayedOut() {
        List<TeamStanding> finished = List.of(team("AAA", "Alpha Club", 120, 84), team("BBB", "Beta Club", 80, 84));

        Map<Award, AwardLeader> leaders = formulas.leaders(LEAGUE, finished, LINES, 84);

        assertThat(leaders.get(Award.ART_ROSS).isFinal()).isTrue();
        assertThat(leaders.get(Award.ROCKET_RICHARD).isFinal()).isTrue();
    }

    private static PlayerStats skater(long id, String name, String position, String team, int gp, int goals, int assists, int plusMinus, int giveaways, int takeaways, int blockedShots, double pkSeconds, int pkGoalsAgainst, boolean rookie) {
        return new PlayerStats(id, name, team, position, rookie, gp, goals, assists, goals + assists, plusMinus, giveaways, takeaways, blockedShots, pkSeconds, pkGoalsAgainst, 0, 0, null, null);
    }

    private static PlayerStats goalie(long id, String name, String team, int gp, int wins, int shutouts, double savePct, double gaa, boolean rookie) {
        return new PlayerStats(id, name, team, "G", rookie, gp, 0, 0, 0, 0, 0, 0, 0, 0, 0, wins, shutouts, savePct, gaa);
    }

    private static TeamStanding team(String code, String name, int points, int gamesPlayed) {
        return new TeamStanding(code, name, "Atlantic", 1, points, gamesPlayed, Instant.EPOCH);
    }

    private static AwardWeights withCalderGoaliePenalty(double penalty) {
        AwardWeights d = AwardWeights.defaults();

        return new AwardWeights(d.minGamesShare(), d.goalWeight(), d.goaliePenalty(),
                d.mvpGoalieSavePct(), d.mvpGoalieGaa(), d.mvpGoalieWins(), d.mvpGoalieShutouts(),
                d.calderGoalWeight(), d.calderDefenseMultiplier(), penalty,
                d.vezinaSavePct(), d.vezinaWins(), d.vezinaGaa(),
                d.norrisProduction(), d.norrisPlusMinus(), d.norrisTakeGive(),
                d.selkeMinPkSeconds(), d.selkeMinPointsPerGame(), d.selkePlusMinus(), d.selkeTakeGive(), d.selkeBlocks(),
                d.selkePkTime(), d.selkePkGoalsAgainst());
    }

    @Test
    void artRossTieOnPointsAndGoalsGoesToFewerGames() {
        List<PlayerStats> tied = List.of(
                skater(1, "More Games",  "C", "AAA", 24, 15, 15, 0, 0, 0, 0, 0, 0, false),
                skater(2, "Fewer Games", "L", "BBB", 23, 15, 15, 0, 0, 0, 0, 0, 0, false));

        assertThat(formulas.leaders(tied, TEAMS, LINES, 84).get(Award.ART_ROSS).playerId()).isEqualTo(2L);
    }

    @Test
    void rocketRichardTieGoesToFewerGames() {
        List<PlayerStats> tied = List.of(
                skater(1, "Tied At 24 Games", "C", "AAA", 24, 18, 20, 0, 0, 0, 0, 0, 0, false),
                skater(2, "Tied At 23 Games", "L", "BBB", 23, 18, 10, 0, 0, 0, 0, 0, 0, false));

        assertThat(formulas.leaders(tied, TEAMS, LINES, 84).get(Award.ROCKET_RICHARD).playerId()).isEqualTo(2L);
    }
}
