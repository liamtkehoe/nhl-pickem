package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.*;

import java.util.*;

public class ScoringEngine {
    public static final int EXACT_PLACE = 2;
    public static final int OFF_BY_ONE = 1;
    public static final int CORRECT_OVER_UNDER = 1;
    public static final int CORRECT_AWARD = 3;

    //Ranking order: total, then exact places, then correct over/unders (all highest first)
    private static final Comparator<PlayerScore> RANKING =
            Comparator.comparingInt(PlayerScore::total).reversed()
                    .thenComparing(Comparator.comparingInt(PlayerScore::exactPlaces).reversed())
                    .thenComparing(Comparator.comparingInt(PlayerScore::correctOverUnders).reversed());

    public PlayerScore score(String username, PickSet picks, SeasonState state) {
        Map<String, TeamStanding> teams = state.teamsByAbbrev();

        List<StandingsResult> standings = picks.standings().stream()
                .map(pick -> scoreStanding(pick, teams.get(pick.team())))
                .toList();
        List<OuResult> overUnders = picks.overUnders().stream()
                .map(pick -> scoreOverUnder(pick, teams.get(pick.team()),
                        state.lines().get(pick.team()), state.gamesInSeason()))
                .toList();
        List<AwardResult> awards = picks.awards().stream()
                .map(pick -> scoreAward(pick, state.leaders().get(pick.award())))
                .toList();

        return PlayerScore.of(username, standings, overUnders, awards);
    }

    public List<LeaderboardRow> leaderboard(List<PlayerScore> scores) {
        // Usernames only break display order; tied players still share a rank.
        List<PlayerScore> sorted = scores.stream()
                .sorted(RANKING.thenComparing(PlayerScore::username, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<LeaderboardRow> rows = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            PlayerScore current = sorted.get(i);
            boolean tiedWithPrevious = i > 0 && RANKING.compare(sorted.get(i - 1), current) == 0;
            int rank = tiedWithPrevious ? rows.get(i - 1).rank() : i + 1;
            rows.add(new LeaderboardRow(rank, current));
        }
        return rows;
    }

    private StandingsResult scoreStanding(StandingsPick pick, TeamStanding team) {
        if (team == null) {
            return new StandingsResult(pick.team(), pick.predictedPlace(), null, 0);
        }
        int distance = Math.abs(pick.predictedPlace() - team.place());
        int points = distance == 0 ? EXACT_PLACE : distance == 1 ? OFF_BY_ONE : 0;
        return new StandingsResult(pick.team(), pick.predictedPlace(), team.place(), points);
    }

    private OuResult scoreOverUnder(OuPick pick, TeamStanding team, Double line, int gamesInSeason) {
        if (team == null || line == null || team.gamesPlayed() == 0) {
            return new OuResult(pick.team(), pick.pick(), line, null, OuStatus.PENDING, 0);
        }

        double projected = (double) team.points() / team.gamesPlayed() * gamesInSeason;
        int gamesLeft = Math.max(0, gamesInSeason - team.gamesPlayed());
        int maxPossible = team.points() + 2 * gamesLeft;

        OverUnder winningSide;
        boolean clinched;
        if (team.points() > line) {
            winningSide = OverUnder.OVER;
            clinched = true;
        } else if (maxPossible < line) {
            winningSide = OverUnder.UNDER;
            clinched = true;
        } else if (Math.abs(projected - line) < 1e-9) {
            return new OuResult(pick.team(), pick.pick(), line, projected, OuStatus.PUSH, 0);
        } else {
            winningSide = projected > line ? OverUnder.OVER : OverUnder.UNDER;
            clinched = false;
        }

        boolean correct = pick.pick() == winningSide;
        OuStatus status = clinched
                ? (correct ? OuStatus.CLINCHED_WIN : OuStatus.CLINCHED_LOSS)
                : (correct ? OuStatus.LEADING : OuStatus.TRAILING);
        return new OuResult(pick.team(), pick.pick(), line, projected, status, correct ? CORRECT_OVER_UNDER : 0);
    }

    private AwardResult scoreAward(AwardPick pick, AwardLeader leader) {
        if (leader == null) {
            return new AwardResult(pick.award(), pick.pickName(), null, false, 0);
        }
        boolean match = pick.award().isTeamPick()
                ? Objects.equals(pick.team(), leader.team())
                : Objects.equals(pick.playerId(), leader.playerId());
        return new AwardResult(pick.award(), pick.pickName(), leader.name(), leader.isFinal(), match ? CORRECT_AWARD : 0);
    }
}
