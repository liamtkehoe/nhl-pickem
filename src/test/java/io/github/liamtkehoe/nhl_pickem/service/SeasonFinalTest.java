package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.liamtkehoe.nhl_pickem.TestData;
import io.github.liamtkehoe.nhl_pickem.config.SeasonSeeder;
import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.repository.AwardResultsRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-pickem.db",
        "pickem.sync.enabled=false"
})
@Transactional
class SeasonFinalTest {
    private static final int SEASON = SeasonSeeder.PRACTICE_SEASON;

    @Autowired SnapshotRepository snapshots;
    @Autowired AwardResultsRepository results;
    @Autowired ScoreService scores;

    @Test
    void finalOnceEveryGameIsPlayedAndEveryWinnerIsRecorded() {
        snapshots.replaceTeams(SEASON, teamsWithGamesPlayed(84));
        recordWinnersExcept(null);

        assertThat(scores.seasonFinalized()).isTrue();
    }

    @Test
    void notFinalBeforeEveryTeamHasPlayedOut() {
        snapshots.replaceTeams(SEASON, teamsWithGamesPlayed(83));
        recordWinnersExcept(null);

        assertThat(scores.seasonFinalized()).isFalse();
    }

    @Test
    void notFinalWhileAnyAwardIsStillProjected() {
        snapshots.replaceTeams(SEASON, teamsWithGamesPlayed(84));
        recordWinnersExcept(Award.HART);

        assertThat(scores.seasonFinalized()).isFalse();
    }

    private static List<TeamStanding> teamsWithGamesPlayed(int games) {
        return TestData.teams().stream()
                .map(t -> new TeamStanding(t.team(), t.teamName(), t.division(), t.place(), t.points(),
                        games, t.fetchedAt()))
                .toList();
    }

    private void recordWinnersExcept(Award skipped) {
        for (Award award : Award.values()) {
            if (award == skipped) {
                continue;
            }
            if (award.isTeamPick()) {
                results.saveFinal(SEASON, award, null, "ANA", "MANUAL", Instant.now());
            } else {
                results.saveFinal(SEASON, award, 1L, null, "MANUAL", Instant.now());
            }
        }
    }
}