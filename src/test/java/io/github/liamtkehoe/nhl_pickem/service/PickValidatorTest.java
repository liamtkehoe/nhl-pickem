package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.liamtkehoe.nhl_pickem.TestData;
import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.AwardPick;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.AwardEntry;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.StandingsEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        standings.set(1, new StandingsEntry("BOS", 1));   //ANA is 1st in that division
        standings.removeLast();                           //WPG left out

        List<AwardEntry> awards = new ArrayList<>(valid.awards());
        awards.replaceAll(a -> a.award() == Award.VEZINA
                ? new AwardEntry(Award.VEZINA, 1L, null)    //a center for the Vezina
                : a);

        SubmitPicksRequest broken = new SubmitPicksRequest(standings, valid.overUnders(), awards);

        assertThatThrownBy(() -> validator.validate(broken, TestData.teams(), TestData.playersById()))
                .isInstanceOfSatisfying(InvalidRequestException.class, e -> assertThat(e.problems())
                        .anyMatch(p -> p.contains("place 1 used twice"))
                        .anyMatch(p -> p.contains("Missing standings pick for WPG"))
                        .anyMatch(p -> p.contains("isn't eligible")));
    }

    @Test
    void calderRejectsANonRookieOnceRookieDataExists() {
        SubmitPicksRequest request = withAward(TestData.perfectRequest(), Award.CALDER, 2L);   //the defenseman

        assertThatThrownBy(() -> validator.validate(request, TestData.teams(), TestData.playersById()))
                .isInstanceOfSatisfying(InvalidRequestException.class, e -> assertThat(e.problems())
                        .anyMatch(p -> p.contains("isn't a rookie")));
    }

    @Test
    void calderAcceptsAnyPlayerBeforeAnyRookieHasPlayed() {
        Map<Long, PlayerInfo> noRookieData = TestData.players().stream()
                .map(p -> new PlayerInfo(p.playerId(), p.name(), p.team(), p.position(), false))
                .collect(Collectors.toMap(PlayerInfo::playerId, Function.identity()));
        SubmitPicksRequest request = withAward(TestData.perfectRequest(), Award.CALDER, 2L);

        PickSet picks = validator.validate(request, TestData.teams(), noRookieData);

        assertThat(picks.awards()).anyMatch(a -> a.award() == Award.CALDER && a.playerId() == 2L);
    }

    private static SubmitPicksRequest withAward(SubmitPicksRequest request, Award award, long playerId) {
        List<AwardEntry> awards = new ArrayList<>(request.awards());
        awards.replaceAll(a -> a.award() == award ? new AwardEntry(award, playerId, null) : a);

        return new SubmitPicksRequest(request.standings(), request.overUnders(), awards);
    }
}
