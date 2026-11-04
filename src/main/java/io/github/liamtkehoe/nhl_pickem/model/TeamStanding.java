package io.github.liamtkehoe.nhl_pickem.model;

import java.time.Instant;

public record TeamStanding(
        String team,
        String teamName,
        String division,
        int place,
        int points,
        int gamesPlayed,
        Instant fetchedAt) {
}
