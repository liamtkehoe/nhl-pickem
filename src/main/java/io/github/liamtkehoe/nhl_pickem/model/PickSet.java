package io.github.liamtkehoe.nhl_pickem.model;

import java.util.List;

public record PickSet(List<StandingsPick> standings, List<OuPick> overUnders, List<AwardPick> awards) {
    public PickSet {
        standings = List.copyOf(standings);
        overUnders = List.copyOf(overUnders);
        awards = List.copyOf(awards);
    }
}
