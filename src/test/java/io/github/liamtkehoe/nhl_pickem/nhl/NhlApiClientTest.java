package io.github.liamtkehoe.nhl_pickem.nhl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NhlApiClientTest {
    private static final String BASE = "https://api-web.nhle.com/v1";

    private MockRestServiceServer server;
    private NhlApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new NhlApiClient(builder.build());
    }

    @Test
    void parsesStandingsAndIgnoresUnknownFields() {
        server.expect(requestTo(BASE + "/standings/now"))
                .andRespond(withSuccess("""
                        {"standings": [{
                            "teamAbbrev": {"default": "PHI"},
                            "teamName": {"default": "Philadelphia Flyers"},
                            "divisionName": "Metropolitan",
                            "divisionSequence": 3,
                            "points": 14,
                            "gamesPlayed": 11,
                            "goalFor": 35
                        }]}
                        """, MediaType.APPLICATION_JSON));

        List<NhlStanding> standings = client.standingsNow();

        assertThat(standings).hasSize(1);
        NhlStanding phi = standings.getFirst();
        assertThat(phi.teamAbbrev().value()).isEqualTo("PHI");
        assertThat(phi.divisionName()).isEqualTo("Metropolitan");
        assertThat(phi.divisionSequence()).isEqualTo(3);
        assertThat(phi.points()).isEqualTo(14);
        server.verify();
    }

    @Test
    void combinesForwardsDefensemenAndGoalies() {
        server.expect(requestTo(BASE + "/roster/PHI/current"))
                .andRespond(withSuccess("""
                        {
                          "forwards":   [{"id": 1, "firstName": {"default": "Fake"}, "lastName": {"default": "Center"}, "positionCode": "C"}],
                          "defensemen": [{"id": 2, "firstName": {"default": "Fake"}, "lastName": {"default": "Dman"}, "positionCode": "D"}],
                          "goalies":    [{"id": 3, "firstName": {"default": "Fake"}, "lastName": {"default": "Goalie"}, "positionCode": "G"}]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<NhlRosterPlayer> roster = client.currentRoster("PHI");

        assertThat(roster).extracting(NhlRosterPlayer::positionCode).containsExactly("C", "D", "G");
        assertThat(roster.getFirst().fullName()).isEqualTo("Fake Center");
        server.verify();
    }
}
