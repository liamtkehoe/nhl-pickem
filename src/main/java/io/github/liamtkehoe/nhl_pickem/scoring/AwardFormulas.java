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

    private record Scale(double mean, double sd) {
        double z(double value) {
            return sd == 0 ? 0 : (value - mean) / sd;
        }

        static Scale of(List<PlayerStats> group, ToDoubleFunction<PlayerStats> stat) {
            double mean = group.stream().mapToDouble(stat).average().orElse(0);
            double variance = group.stream()
                    .mapToDouble(p -> Math.pow(stat.applyAsDouble(p) - mean, 2))
                    .average()
                    .orElse(0);

            return new Scale(mean, Math.sqrt(variance));
        }
    }

    public record Candidate(Long playerId, String team, String name, String position, double score) {}

    public Map<Award, AwardLeader> leaders(List<PlayerStats> players, List<TeamStanding> teams, Map<String, Double> lines, int gamesInSeason) {
        boolean seasonOver = !teams.isEmpty() && teams.stream().allMatch(t -> t.gamesPlayed() >= gamesInSeason);

        Map<Award, AwardLeader> leaders = new EnumMap<>(Award.class);
        rankings(players, teams, lines, gamesInSeason).forEach((award, ranking) -> {
            if (ranking.isEmpty()) {
                return;
            }
            Candidate top = ranking.getFirst();
            boolean isFinal = seasonOver && (award == Award.ART_ROSS || award == Award.ROCKET_RICHARD);
            leaders.put(award, award.isTeamPick()
                    ? new AwardLeader(award, null, top.team(), top.name(), false)
                    : new AwardLeader(award, top.playerId(), null, top.name(), isFinal));
        });

        return leaders;
    }

    public Map<Award, List<Candidate>> rankings(List<PlayerStats> players, List<TeamStanding> teams, Map<String, Double> lines, int gamesInSeason) {
        Map<String, Integer> teamGames = teams.stream()
                .collect(Collectors.toMap(TeamStanding::team, TeamStanding::gamesPlayed));
        int mostTeamGames = teams.stream().mapToInt(TeamStanding::gamesPlayed).max().orElse(0);

        List<PlayerStats> allSkaters = players.stream().filter(p -> !p.isGoalie()).toList();
        List<PlayerStats> eligible = players.stream()
                .filter(p -> playedEnough(p, teamGames, mostTeamGames))
                .toList();
        List<PlayerStats> skaters = eligible.stream().filter(p -> !p.isGoalie()).toList();
        List<PlayerStats> goalies = eligible.stream().filter(PlayerStats::isGoalie).toList();
        List<PlayerStats> defensemen = skaters.stream().filter(p -> "D".equals(p.position())).toList();
        List<PlayerStats> selkeCandidates = skaters.stream()
                .filter(PlayerStats::isForward)
                .filter(p -> p.pkSecondsPerGame() >= w.selkeMinPkSeconds())
                .filter(p -> pointsPerGame(p) >= w.selkeMinPointsPerGame())
                .toList();

        Map<Award, List<Candidate>> rankings = new EnumMap<>(Award.class);

        rankings.put(Award.ART_ROSS, rankInOrder(allSkaters,
                Comparator.comparingInt(PlayerStats::points).reversed()                         //most points
                        .thenComparing(Comparator.comparingInt(PlayerStats::goals).reversed())  //then most goals
                        .thenComparingInt(PlayerStats::gamesPlayed),                            //then fewest games
                PlayerStats::points));
        rankings.put(Award.ROCKET_RICHARD, rankInOrder(allSkaters,
                Comparator.comparingInt(PlayerStats::goals).reversed()                          //most goals
                        .thenComparingInt(PlayerStats::gamesPlayed),                            //then fewest games
                PlayerStats::goals));

        rankings.put(Award.HART, mostValuable(skaters, goalies, skaters, goalies, this::production, w.goaliePenalty()));
        rankings.put(Award.CALDER, mostValuable(rookies(skaters), rookies(goalies), skaters, goalies, this::calderProduction, w.calderGoaliePenalty()));

        rankings.put(Award.VEZINA, rankByScores(goalies, scores(goalies, goalies,
                new Part(w.vezinaSavePct(), p -> orZero(p.savePct())),
                new Part(w.vezinaWins(), PlayerStats::wins),
                new Part(w.vezinaGaa(), p -> -orZero(p.gaa())))));

        rankings.put(Award.NORRIS, rankByScores(defensemen, scores(defensemen, defensemen,
                new Part(w.norrisProduction(), this::production),
                new Part(w.norrisPlusMinus(), PlayerStats::plusMinus),
                new Part(w.norrisTakeGive(), AwardFormulas::takeMinusGive))));

        rankings.put(Award.SELKE, rankByScores(selkeCandidates, scores(selkeCandidates, selkeCandidates,
                new Part(w.selkePlusMinus(), PlayerStats::plusMinus),
                new Part(w.selkeTakeGive(), AwardFormulas::takeMinusGive),
                new Part(w.selkeBlocks(), PlayerStats::blockedShots),
                new Part(w.selkePkTime(), PlayerStats::pkSecondsPerGame),
                new Part(w.selkePkGoalsAgainst(), p -> -pkGoalsAgainstPer60(p)))));

        rankings.put(Award.JACK_ADAMS, jackAdams(teams, lines, gamesInSeason));

        return rankings;
    }

    private static List<Candidate> rankInOrder(List<PlayerStats> group, Comparator<PlayerStats> bestFirst, ToDoubleFunction<PlayerStats> shown) {
        return group.stream()
                .sorted(bestFirst)
                .map(p -> new Candidate(p.playerId(), p.team(), p.name(), p.position(), shown.applyAsDouble(p)))
                .toList();
    }

    private static double pointsPerGame(PlayerStats p) {
        return p.gamesPlayed() == 0 ? 0 : (double) p.points() / p.gamesPlayed();
    }

    private List<Candidate> mostValuable(List<PlayerStats> skaterCandidates, List<PlayerStats> goalieCandidates, List<PlayerStats> skaterBaseline, List<PlayerStats> goalieBaseline, ToDoubleFunction<PlayerStats> production, double goaliePenalty) {
        Map<Long, Double> scores = new HashMap<>(scores(skaterCandidates, skaterBaseline, new Part(1.0, production)));
        scores(goalieCandidates, goalieBaseline, goalieParts())
                .forEach((id, score) -> scores.put(id, score - goaliePenalty));

        List<PlayerStats> everyone = new ArrayList<>(skaterCandidates);
        everyone.addAll(goalieCandidates);

        return rankByScores(everyone, scores);
    }

    private Part[] goalieParts() {
        return new Part[] {
                new Part(w.mvpGoalieSavePct(), p -> orZero(p.savePct())),
                new Part(w.mvpGoalieGaa(), p -> -orZero(p.gaa())),
                new Part(w.mvpGoalieWins(), PlayerStats::wins),
                new Part(w.mvpGoalieShutouts(), PlayerStats::shutouts),
        };
    }

    private static List<Candidate> jackAdams(List<TeamStanding> teams, Map<String, Double> lines, int gamesInSeason) {
        return teams.stream()
                .filter(t -> t.gamesPlayed() > 0 && lines.containsKey(t.team()))
                .map(t -> new Candidate(null, t.team(), t.teamName(), null,
                        (double) t.points() / t.gamesPlayed() * gamesInSeason - lines.get(t.team())))
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .toList();
    }

    private static Map<Long, Double> scores(List<PlayerStats> candidates, List<PlayerStats> baseline, Part... parts) {
        Map<Long, Double> total = new HashMap<>();
        for (Part part : parts) {
            Scale scale = Scale.of(baseline, part.stat());
            for (PlayerStats p : candidates) {
                total.merge(p.playerId(), part.weight() * scale.z(part.stat().applyAsDouble(p)), Double::sum);
            }
        }

        return total;
    }

    private static List<Candidate> rankByScores(List<PlayerStats> group, Map<Long, Double> scores) {
        return rankBy(group, p -> scores.getOrDefault(p.playerId(), Double.NEGATIVE_INFINITY));
    }

    private static List<Candidate> rankBy(List<PlayerStats> group, ToDoubleFunction<PlayerStats> score) {
        return group.stream()
                .map(p -> new Candidate(p.playerId(), p.team(), p.name(), p.position(), score.applyAsDouble(p)))
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .toList();
    }

    private boolean playedEnough(PlayerStats p, Map<String, Integer> teamGames, int mostTeamGames) {
        int teamGamesPlayed = teamGames.getOrDefault(p.team(), mostTeamGames);

        return teamGamesPlayed > 0 && p.gamesPlayed() >= w.minGamesShare() * teamGamesPlayed;
    }

    private double production(PlayerStats p) {
        return w.goalWeight() * p.goals() + p.assists();
    }

    private double calderProduction(PlayerStats p) {
        double base = w.calderGoalWeight() * p.goals() + p.assists();

        return "D".equals(p.position()) ? base * w.calderDefenseMultiplier() : base;
    }

    private static double takeMinusGive(PlayerStats p) {
        return p.takeaways() - p.giveaways();
    }

    private static double pkGoalsAgainstPer60(PlayerStats p) {
        double pkMinutes = p.pkSecondsPerGame() * p.gamesPlayed() / 60.0;

        return pkMinutes == 0 ? 0 : p.pkGoalsAgainst() * 60.0 / pkMinutes;
    }

    private static List<PlayerStats> rookies(List<PlayerStats> group) {
        return group.stream().filter(PlayerStats::rookie).toList();
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }
}
