package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RosterResponse(
        List<NhlRosterPlayer> forwards,
        List<NhlRosterPlayer> defensemen,
        List<NhlRosterPlayer> goalies) {
}
