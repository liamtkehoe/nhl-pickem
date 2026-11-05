package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.OverUnder;

public record OuResult(
        String team,
        OverUnder pick,
        Double line,
        Double projectedPoints,
        OuStatus status,
        int points) {
}
