package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One player in /v1/roster/{team}/current. positionCode is C, L, R, D or G. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NhlRosterPlayer(long id, NhlText firstName, NhlText lastName, String positionCode) {
    public String fullName() {
        return firstName.value() + " " + lastName.value();
    }
}
