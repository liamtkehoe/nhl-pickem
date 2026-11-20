package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PlayerStats;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class AwardFormulas {
    private final AwardWeights w;

    public AwardFormulas(AwardWeights weights) {
        this.w = weights;
    }

    private record Part(double weight, ToDoubleFunction<PlayerStats> stat) {}

    public Map<Award, AwardLeader> leaders(List<PlayerStats> players, List<TeamStanding> teams, Map<String, Double> lines, int gamesInSeason) {
        Map<String, Integer> teamGames = teams.stream()
                .collect(Collectors.toMap(TeamStanding::team, TeamStanding::gamesPlayed));
        int mostTeamGames = teams.stream().mapToInt(TeamStanding::gamesPlayed).max().orElse(0);
        boolean seasonOver = mostTeamGames > 0
                && teams.stream().allMatch(t -> t.gamesPlayed() >= gamesInSeason);

        List<PlayerStats> allSkaters = players.stream().filter(p -> !p.isGoalie()).toList();
        List<PlayerStats> eligible = players.stream()
                .filter(p -> playedEnough(p, teamGames, mostTeamGames))
                .toList();
        List<PlayerStats> skaters = eligible.stream().filter(p -> !p.isGoalie()).toList();
        List<PlayerStats> goalies = eligible.stream().filter(PlayerStats::isGoalie).toList();
        List<PlayerStats> defensemen = skaters.stream().filter(p -> "D".equals(p.position())).toList();
        List<PlayerStats> forwards = skaters.stream().filter(PlayerStats::isForward).toList();

        Map<Award, AwardLeader> leaders = new EnumMap<>(Award.class);
        putLeader(leaders, Award.ART_ROSS, seasonOver, allSkaters.stream()
                .max(Comparator.comparingInt(PlayerStats::points).thenComparingInt(PlayerStats::goals)));
        putLeader(leaders, Award.ROCKET_RICHARD, seasonOver, allSkaters.stream()
                .max(Comparator.comparingInt(PlayerStats::goals).thenComparingInt(PlayerStats::points)));
        putLeader(leaders, Award.HART, false, mostValuable(skaters, goalies));
        putLeader(leaders, Award.CALDER, false, mostValuable(rookies(skaters), rookies(goalies)));
        putLeader(leaders, Award.VEZINA, false, best(goalies,
                new Part(w.vezinaSavePct(), p -> orZero(p.savePct())),
                new Part(w.vezinaWins(), PlayerStats::wins),
                new Part(w.vezinaGaa(), p -> -orZero(p.gaa()))));     // lower GAA is better
        putLeader(leaders, Award.NORRIS, false, best(defensemen,
                new Part(w.norrisProduction(), this::production),
                new Part(w.norrisPlusMinus(), PlayerStats::plusMinus),
                new Part(w.norrisTakeGive(), AwardFormulas::takeMinusGive)));
        putLeader(leaders, Award.SELKE, false, best(forwards,
                new Part(w.selkePlusMinus(), PlayerStats::plusMinus),
                new Part(w.selkeTakeGive(), AwardFormulas::takeMinusGive)));
        jackAdams(teams, lines, gamesInSeason).ifPresent(l -> leaders.put(Award.JACK_ADAMS, l));

        return leaders;
    }

    private Optional<PlayerStats> mostValuable(List<PlayerStats> skaters, List<PlayerStats> goalies) {
        Map<Long, Double> scores = new HashMap<>(scores(skaters, new Part(1.0, this::production)));
        scores(goalies,
                new Part(0.5, p -> orZero(p.savePct())),
                new Part(0.5, PlayerStats::wins))
                .forEach((id, score) -> scores.put(id, score - w.goaliePenalty()));

        List<PlayerStats> everyone = new ArrayList<>(skaters);
        everyone.addAll(goalies);

        return highest(everyone, scores);
    }

    //Jack Adams calculated based on team furthest above it's O/U line
    private Optional<AwardLeader> jackAdams(List<TeamStanding> teams, Map<String, Double> lines, int gamesInSeason) {
        return teams.stream()
                .filter(t -> t.gamesPlayed() > 0 && lines.containsKey(t.team()))
                .max(Comparator.comparingDouble((TeamStanding t) ->
                        (double) t.points() / t.gamesPlayed() * gamesInSeason - lines.get(t.team())))
                .map(t -> new AwardLeader(Award.JACK_ADAMS, null, t.team(), t.teamName(), false));
    }

    private Optional<PlayerStats> best(List<PlayerStats> group, Part... parts) {
        return highest(group, scores(group, parts));
    }

    private static Map<Long, Double> scores(List<PlayerStats> group, Part... parts) {
        Map<Long, Double> total = new HashMap<>();
        for (Part part : parts) {
            zScores(group, part.stat())
                    .forEach((id, z) -> total.merge(id, part.weight() * z, Double::sum));
        }

        return total;
    }

    static Map<Long, Double> zScores(List<PlayerStats> group, ToDoubleFunction<PlayerStats> stat) {
        double mean = group.stream().mapToDouble(stat).average().orElse(0);
        double variance = group.stream()
                .mapToDouble(p -> Math.pow(stat.applyAsDouble(p) - mean, 2))
                .average()
                .orElse(0);
        double sd = Math.sqrt(variance);

        Map<Long, Double> z = new HashMap<>();
        for (PlayerStats p : group) {
            z.put(p.playerId(), sd == 0 ? 0 : (stat.applyAsDouble(p) - mean) / sd);
        }

        return z;
    }

    private static Optional<PlayerStats> highest(List<PlayerStats> group, Map<Long, Double> scores) {
        return group.stream()
                .max(Comparator.comparingDouble((PlayerStats p) ->
                        scores.getOrDefault(p.playerId(), Double.NEGATIVE_INFINITY)));
    }

    private boolean playedEnough(PlayerStats p, Map<String, Integer> teamGames, int mostTeamGames) {
        int teamGamesPlayed = teamGames.getOrDefault(p.team(), mostTeamGames);

        return teamGamesPlayed > 0 && p.gamesPlayed() >= w.minGamesShare() * teamGamesPlayed;
    }

    private double production(PlayerStats p) {
        return w.goalWeight() * p.goals() + p.assists();
    }

    private static double takeMinusGive(PlayerStats p) {
        return p.takeaways() - p.giveaways();
    }

    private static List<PlayerStats> rookies(List<PlayerStats> group) {
        return group.stream().filter(PlayerStats::rookie).toList();
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }

    private static void putLeader(Map<Award, AwardLeader> leaders, Award award, boolean isFinal, Optional<PlayerStats> leader) {
        leader.ifPresent(p -> leaders.put(award,
                new AwardLeader(award, p.playerId(), null, p.name(), isFinal)));
    }
}
