package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoalieSummary(
        long playerId,
        String goalieFullName,
        String teamAbbrevs,
        int gamesPlayed,
        int wins,
        Integer shutouts,
        Double savePct,
        Double goalsAgainstAverage) {
}
