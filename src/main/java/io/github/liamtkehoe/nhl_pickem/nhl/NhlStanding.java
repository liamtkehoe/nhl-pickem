package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One team's entry in /v1/standings/now. divisionSequence is the team's place in its division. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NhlStanding(
        NhlText teamAbbrev,
        NhlText teamName,
        String divisionName,
        int divisionSequence,
        int points,
        int gamesPlayed) {
}
