package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.liamtkehoe.nhl_pickem.config.SeasonSeeder;
import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.AwardPick;
import io.github.liamtkehoe.nhl_pickem.model.OuPick;
import io.github.liamtkehoe.nhl_pickem.model.OverUnder;
import io.github.liamtkehoe.nhl_pickem.model.PickSet;
import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.model.StandingsPick;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.PickRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import io.github.liamtkehoe.nhl_pickem.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.datasource.url=jdbc:sqlite:target/test-pickem.db")
@Transactional
class PickServiceTest {
    private static final int SEASON = SeasonSeeder.PRACTICE_SEASON;
    private static final List<String> TEAMS = List.of(
            "ANA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ",
            "DAL", "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH",
            "NJD", "NYI", "NYR", "OTT", "PHI", "PIT", "SJS", "SEA",
            "STL", "TBL", "TOR", "UTA", "VAN", "VGK", "WSH", "WPG");

    @Autowired PickService pickService;
    @Autowired PickRepository pickRepository;
    @Autowired SeasonRepository seasonRepository;
    @Autowired UserRepository userRepository;

    private User user;

    @BeforeEach
    void createUser() {
        user = userRepository.create("testuser", Instant.now());
    }

    @Test
    void savesAndReadsBackAFullPickSet() {
        pickService.submit(user.id(), SEASON, samplePicks());

        PickSet saved = pickRepository.find(user.id(), SEASON);
        assertThat(saved.standings()).hasSize(32);
        assertThat(saved.overUnders()).hasSize(32);
        assertThat(saved.awards()).hasSize(Award.values().length);
        assertThat(saved.awards()).contains(AwardPick.team(Award.JACK_ADAMS, "PHI", "PHI head coach"));
        assertThat(pickRepository.submittedAt(user.id(), SEASON)).isPresent();
    }

    @Test
    void refusesASecondSubmission() {
        pickService.submit(user.id(), SEASON, samplePicks());

        assertThatThrownBy(() -> pickService.submit(user.id(), SEASON, samplePicks()))
                .isInstanceOf(PickException.class)
                .hasMessageContaining("already submitted");
    }

    @Test
    void refusesSubmissionsAfterTheCloseDate() {
        seasonRepository.save(new Season(20252026, false, Instant.parse("2025-10-01T00:00:00Z")));

        assertThatThrownBy(() -> pickService.submit(user.id(), 20252026, samplePicks()))
                .isInstanceOf(PickException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void usernamesAreCaseInsensitive() {
        assertThat(userRepository.findByUsername("TESTUSER")).isPresent();

        assertThatThrownBy(() -> userRepository.create("TestUser", Instant.now()))
                .isInstanceOf(DataAccessException.class);
    }

    private static PickSet samplePicks() {
        List<StandingsPick> standings = new ArrayList<>();
        List<OuPick> overUnders = new ArrayList<>();
        for (int i = 0; i < TEAMS.size(); i++) {
            standings.add(new StandingsPick(TEAMS.get(i), i % 8 + 1));
            overUnders.add(new OuPick(TEAMS.get(i), i % 2 == 0 ? OverUnder.OVER : OverUnder.UNDER));
        }

        List<AwardPick> awards = new ArrayList<>();
        long playerId = 8_470_000;
        for (Award award : Award.values()) {
            awards.add(award.isTeamPick()
                    ? AwardPick.team(award, "PHI", "PHI head coach")
                    : AwardPick.player(award, playerId++, "Player " + award.name()));
        }
        return new PickSet(standings, overUnders, awards);
    }
}
