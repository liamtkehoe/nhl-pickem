package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.liamtkehoe.nhl_pickem.TestData;
import io.github.liamtkehoe.nhl_pickem.config.SeasonSeeder;
import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.OverUnder;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-pickem.db",
        "pickem.sync.enabled=false"
})
@Transactional
class AdminServiceTest {
    private static final int SEASON = SeasonSeeder.PRACTICE_SEASON;

    @Autowired SnapshotRepository snapshots;
    @Autowired UserService userService;
    @Autowired PickService pickService;
    @Autowired PickRepository picks;
    @Autowired AdminService admin;

    @BeforeEach
    void setUpLeague() {
        snapshots.replaceTeams(SEASON, TestData.teams());
        snapshots.upsertPlayers(SEASON, TestData.players(), Instant.now());
    }

    @Test
    void finalizeIsRefusedUntilEveryAwardHasAWinner() {
        recordWinnersExcept(Award.HART);

        assertThatThrownBy(() -> admin.finalizeSeason("admin"))
                .isInstanceOfSatisfying(InvalidRequestException.class, e -> assertThat(e.problems())
                        .containsExactly("Record a winner for the Hart Memorial Trophy"));
        assertThat(admin.isFinalized()).isFalse();
    }

    @Test
    void finalizeAndUnfinalize() {
        recordWinnersExcept(null);

        admin.finalizeSeason("admin");
        assertThat(admin.isFinalized()).isTrue();

        admin.unfinalizeSeason();
        assertThat(admin.isFinalized()).isFalse();
    }

    @Test
    void playersPicksAndWinnersAreLockedWhileFinalized() {
        User player = userService.signUp("player");
        pickService.submit(player.id(), SEASON, TestData.perfectRequest());
        recordWinnersExcept(null);
        admin.finalizeSeason("admin");

        assertThatThrownBy(() -> admin.replacePicks("player", TestData.backwardsRequest()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> admin.deleteUser("player", "admin"))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> admin.recordWinner(new AwardLeaderService.WinnerRequest(Award.HART, 1L, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void replacePicksSwapsThePicksButKeepsTheSubmissionDate() {
        User player = userService.signUp("player");
        Instant submitted = pickService.submit(player.id(), SEASON, TestData.perfectRequest());

        admin.replacePicks("player", TestData.backwardsRequest());

        PickSet saved = picks.find(player.id(), SEASON);
        assertThat(saved.overUnders()).allMatch(p -> p.pick() == OverUnder.UNDER);
        assertThat(picks.submittedAt(player.id(), SEASON)).contains(submitted);
    }

    @Test
    void deleteUserRemovesTheAccountAndEveryPick() {
        User player = userService.signUp("player");
        pickService.submit(player.id(), SEASON, TestData.perfectRequest());

        admin.deleteUser("player", "admin");

        assertThatThrownBy(() -> userService.logIn("player")).isInstanceOf(NotFoundException.class);
        PickSet leftovers = picks.find(player.id(), SEASON);
        assertThat(leftovers.standings()).isEmpty();
        assertThat(leftovers.overUnders()).isEmpty();
        assertThat(leftovers.awards()).isEmpty();
        assertThat(picks.submittedAt(player.id(), SEASON)).isEmpty();
    }

    @Test
    void anAdminCannotDeleteTheirOwnAccount() {
        userService.signUp("boss");

        assertThatThrownBy(() -> admin.deleteUser("boss", "Boss"))
                .isInstanceOf(InvalidRequestException.class);
    }

    private void recordWinnersExcept(Award skipped) {
        for (Award award : Award.values()) {
            if (award == skipped) {
                continue;
            }
            admin.recordWinner(award.isTeamPick()
                    ? new AwardLeaderService.WinnerRequest(award, null, "PHI")
                    : new AwardLeaderService.WinnerRequest(award, 1L, null));
        }
    }
}
