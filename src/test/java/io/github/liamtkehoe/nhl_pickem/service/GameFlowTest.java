package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.liamtkehoe.nhl_pickem.TestData;
import io.github.liamtkehoe.nhl_pickem.config.SeasonSeeder;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-pickem.db",
        "pickem.sync.enabled=false"
})
@Transactional
class GameFlowTest {
    private static final int SEASON = SeasonSeeder.PRACTICE_SEASON;

    @Autowired SnapshotRepository snapshots;
    @Autowired OULinesService linesService;
    @Autowired UserService userService;
    @Autowired PickService pickService;
    @Autowired ScoreService scoreService;

    @Test
    void twoPlayersSubmitAndGetRanked() {
        snapshots.replaceTeams(SEASON, TestData.teams());
        snapshots.upsertPlayers(SEASON, TestData.players(), Instant.now());
        Map<String, Double> lines = TestData.teams().stream()
                .collect(Collectors.toMap(TeamStanding::team, t -> 90.5));
        linesService.importLines(new OULinesImport(SEASON, "test", "2026-07-20", lines));

        User perfect = userService.login("perfect").user();
        User backwards = userService.login("backwards").user();
        pickService.submit(perfect.id(), SEASON, TestData.perfectRequest());
        pickService.submit(backwards.id(), SEASON, TestData.backwardsRequest());

        List<LeaderboardEntry> board = scoreService.leaderboard();

        assertThat(board).extracting(LeaderboardEntry::username).containsExactly("perfect", "backwards");
        assertThat(board.get(0).total()).isEqualTo(96);
        assertThat(board.get(0).exactPlaces()).isEqualTo(32);
        assertThat(board.get(1).total()).isEqualTo(8);
        assertThat(board).allSatisfy(row -> assertThat(row.submittedAt()).isNotNull());
    }
}
