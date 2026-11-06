package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.OverUnder;

import java.util.List;

public record SubmitPicksRequest(List<StandingsEntry> standings, List<OuEntry> overUnders, List<AwardEntry> awards) {
    public record StandingsEntry(String team, int place) {}

    public record OuEntry(String team, OverUnder pick) {}

    //playerId for player awards, team for Jack Adams
    public record AwardEntry(Award award, Long playerId, String team) {}
}
