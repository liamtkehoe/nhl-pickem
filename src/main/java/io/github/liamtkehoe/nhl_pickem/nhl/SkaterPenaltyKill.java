package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SkaterPenaltyKill(long playerId, Double shTimeOnIcePerGame, Integer shGoalsAgainst) {
}
