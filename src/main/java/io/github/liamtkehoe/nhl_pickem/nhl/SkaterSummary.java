package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SkaterSummary(
        long playerId,
        String skaterFullName,
        String positionCode,
        String teamAbbrevs,
        int gamesPlayed,
        int goals,
        int assists,
        int points,
        int plusMinus) {
}
