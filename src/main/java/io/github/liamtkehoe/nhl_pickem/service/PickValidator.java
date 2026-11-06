package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PickValidator {
    public PickSet validate(SubmitPicksRequest request, List<TeamStanding> teams, Map<Long, PlayerInfo> players) {
        Map<String, TeamStanding> byTeam = teams.stream()
                .collect(Collectors.toMap(TeamStanding::team, Function.identity(), (a, b) -> a, TreeMap::new));
        if (byTeam.isEmpty()) {
            throw new InvalidRequestException(List.of("No standings yet. Try again after the NHL sync runs"));
        }

        List<String> problems = new ArrayList<>();
        List<StandingsPick> standings = validateStandings(request.standings(), byTeam, problems);
        List<OuPick> overUnders = validateOverUnders(request.overUnders(), byTeam, problems);
        List<AwardPick> awards = validateAwards(request.awards(), byTeam, players, problems);

        if (!problems.isEmpty()) {
            throw new InvalidRequestException(problems);
        }
        return new PickSet(standings, overUnders, awards);
    }

    private List<StandingsPick> validateStandings(List<SubmitPicksRequest.StandingsEntry> entries, Map<String, TeamStanding> byTeam, List<String> problems) {
        List<StandingsPick> picks = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Map<String, Set<Integer>> placesUsed = new HashMap<>();

        for (SubmitPicksRequest.StandingsEntry entry : orEmpty(entries)) {
            TeamStanding team = byTeam.get(entry.team());
            if (team == null) {
                problems.add("Unknown team in standings: " + entry.team());
            } else if (!seen.add(entry.team())) {
                problems.add("Team ranked twice: " + entry.team());
            } else if (entry.place() < 1 || entry.place() > 8) {
                problems.add(entry.team() + ": place must be 1-8");
            } else if (!placesUsed.computeIfAbsent(team.division(), d -> new HashSet<>()).add(entry.place())) {
                problems.add(team.division() + ": place " + entry.place() + " used twice");
            } else {
                picks.add(new StandingsPick(entry.team(), entry.place()));
            }
        }
        for (String team : byTeam.keySet()) {
            if (!seen.contains(team)) {
                problems.add("Missing standings pick for " + team);
            }
        }
        return picks;
    }

    private List<OuPick> validateOverUnders(List<SubmitPicksRequest.OuEntry> entries, Map<String, TeamStanding> byTeam, List<String> problems) {
        List<OuPick> picks = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (SubmitPicksRequest.OuEntry entry : orEmpty(entries)) {
            if (!byTeam.containsKey(entry.team())) {
                problems.add("Unknown team in over/unders: " + entry.team());
            } else if (entry.pick() == null) {
                problems.add(entry.team() + ": pick OVER or UNDER");
            } else if (!seen.add(entry.team())) {
                problems.add("Over/under picked twice: " + entry.team());
            } else {
                picks.add(new OuPick(entry.team(), entry.pick()));
            }
        }
        for (String team : byTeam.keySet()) {
            if (!seen.contains(team)) {
                problems.add("Missing over/under pick for " + team);
            }
        }
        return picks;
    }

    private List<AwardPick> validateAwards(List<SubmitPicksRequest.AwardEntry> entries, Map<String, TeamStanding> byTeam, Map<Long, PlayerInfo> players, List<String> problems) {
        List<AwardPick> picks = new ArrayList<>();
        Set<Award> seen = EnumSet.noneOf(Award.class);

        for (SubmitPicksRequest.AwardEntry entry : orEmpty(entries)) {
            Award award = entry.award();
            if (award == null) {
                problems.add("Award pick without an award");
            } else if (!seen.add(award)) {
                problems.add(award.displayName() + " picked twice");
            } else if (award.isTeamPick()) {
                TeamStanding team = byTeam.get(entry.team());
                if (team == null) {
                    problems.add(award.displayName() + ": unknown team " + entry.team());
                } else {
                    picks.add(AwardPick.team(award, team.team(), team.teamName()));
                }
            } else {
                PlayerInfo player = entry.playerId() == null ? null : players.get(entry.playerId());
                if (player == null) {
                    problems.add(award.displayName() + ": unknown player " + entry.playerId());
                } else if (!award.allowsPosition(player.position())) {
                    problems.add(award.displayName() + ": " + player.name() + " (" + player.position() + ") isn't eligible");
                } else {
                    picks.add(AwardPick.player(award, player.playerId(), player.name()));
                }
            }
        }
        for (Award award : Award.values()) {
            if (!seen.contains(award)) {
                problems.add("Missing pick for the " + award.displayName());
            }
        }
        return picks;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
