package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.liamtkehoe.nhl_pickem.TestData;
import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.AwardPick;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.AwardEntry;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.StandingsEntry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PickValidatorTest {
    private final PickValidator validator = new PickValidator();

    @Test
    void acceptsACompleteSubmissionAndUsesServerSideNames() {
        PickSet picks = validator.validate(TestData.perfectRequest(), TestData.teams(), TestData.playersById());

        assertThat(picks.standings()).hasSize(32);
        assertThat(picks.overUnders()).hasSize(32);
        assertThat(picks.awards()).hasSize(Award.values().length);
        assertThat(picks.awards())
                .filteredOn(a -> a.award() == Award.VEZINA)
                .singleElement()
                .extracting(AwardPick::pickName)
                .isEqualTo("Fake Goalie");
    }

    @Test
    void reportsEveryProblemAtOnce() {
        SubmitPicksRequest valid = TestData.perfectRequest();

        List<StandingsEntry> standings = new ArrayList<>(valid.standings());
        standings.set(1, new StandingsEntry("BOS", 1));   // ANA is 1st in that division
        standings.removeLast();                           // WPG left out

        List<AwardEntry> awards = new ArrayList<>(valid.awards());
        awards.replaceAll(a -> a.award() == Award.VEZINA
                ? new AwardEntry(Award.VEZINA, 1L, null)    // a center for the Vezina
                : a);

        SubmitPicksRequest broken = new SubmitPicksRequest(standings, valid.overUnders(), awards);

        assertThatThrownBy(() -> validator.validate(broken, TestData.teams(), TestData.playersById()))
                .isInstanceOfSatisfying(InvalidRequestException.class, e -> assertThat(e.problems())
                        .anyMatch(p -> p.contains("place 1 used twice"))
                        .anyMatch(p -> p.contains("Missing standings pick for WPG"))
                        .anyMatch(p -> p.contains("isn't eligible")));
    }
}
