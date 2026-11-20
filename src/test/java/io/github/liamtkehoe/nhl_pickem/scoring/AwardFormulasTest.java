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

    private static final AwardWeights DEFAULTS =
            new AwardWeights(0.5, 2.0, 1.0, 0.6, 0.25, 0.15, 0.75, 0.15, 0.10, 0.7, 0.3);

    private final AwardFormulas formulas = new AwardFormulas(DEFAULTS);

    private static final List<PlayerStats> LEAGUE = List.of(
            skater(1, "Alpha Center",   "C", "AAA", 20, 30, 30,  12, 10, 20, false),
            skater(2, "Beta Wing",      "L", "BBB", 20, 12, 18,   2,  8,  6, true),
            skater(3, "Gamma Wing",     "R", "BBB", 20, 10, 10,  -4, 12,  5, true),
            skater(4, "Delta Defense",  "D", "AAA", 20,  5, 20,  15,  6, 10, false),
            skater(5, "Echo Defense",   "D", "BBB", 20,  8, 25,  -6, 15,  4, false),
            skater(6, "Fox Callup",     "C", "AAA",  3,  4,  4,  20,  0, 15, true),
            goalie(7, "Goalie One",  "AAA", 18, 14, 0.930, 2.10, false),
            goalie(8, "Goalie Two",  "BBB", 17,  8, 0.900, 3.00, false),
            goalie(9, "Hot Backup",  "AAA",  3,  3, 1.000, 0.00, true));

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
                skater(1, "Playmaker", "C", "AAA", 20, 10, 20, 0, 0, 0, false),
                skater(2, "Sniper",    "L", "AAA", 20, 15, 15, 0, 0, 0, false));

        assertThat(formulas.leaders(tied, TEAMS, LINES, 84).get(Award.ART_ROSS).playerId()).isEqualTo(2L);
    }

    @Test
    void hartFavorsADominantSkaterOverAGoodGoalie() {
        assertThat(leaders().get(Award.HART).playerId()).isEqualTo(1L);
    }

    @Test
    void calderOnlyConsidersEligibleRookies() {
        assertThat(leaders().get(Award.CALDER).playerId()).isEqualTo(2L);
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
    void selkeIgnoresForwardsBelowHalfTheirTeamsGames() {
        assertThat(leaders().get(Award.SELKE).playerId()).isEqualTo(1L);
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

    private static PlayerStats skater(long id, String name, String position, String team, int gp, int goals, int assists, int plusMinus, int giveaways, int takeaways, boolean rookie) {
        return new PlayerStats(id, name, team, position, rookie, gp, goals, assists, goals + assists, plusMinus, giveaways, takeaways, 0, null, null);
    }

    private static PlayerStats goalie(long id, String name, String team, int gp, int wins, double savePct, double gaa, boolean rookie) {
        return new PlayerStats(id, name, team, "G", rookie, gp, 0, 0, 0, 0, 0, 0, wins, savePct, gaa);
    }

    private static TeamStanding team(String code, String name, int points, int gamesPlayed) {
        return new TeamStanding(code, name, "Atlantic", 1, points, gamesPlayed, Instant.EPOCH);
    }
}
