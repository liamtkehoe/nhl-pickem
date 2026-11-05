package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SeasonState(
        List<TeamStanding> standings,
        Map<String, Double> lines,          // team -> over/under line
        Map<Award, AwardLeader> leaders,
        int gamesInSeason) {

    public Map<String, TeamStanding> teamsByAbbrev() {
        return standings.stream().collect(Collectors.toMap(TeamStanding::team, Function.identity()));
    }
}
