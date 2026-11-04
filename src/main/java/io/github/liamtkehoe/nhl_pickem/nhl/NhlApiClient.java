package io.github.liamtkehoe.nhl_pickem.nhl;

import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

public class NhlApiClient {
    private final RestClient http;

    public NhlApiClient(RestClient http) {
        this.http = http;
    }

    public List<NhlStanding> standingsNow() {
        StandingsResponse body = http.get()
                .uri("/standings/now")
                .retrieve()
                .body(StandingsResponse.class);
        return body == null || body.standings() == null ? List.of() : body.standings();
    }

    /** Forwards, defensemen and goalies combined into one list. */
    public List<NhlRosterPlayer> currentRoster(String team) {
        RosterResponse body = http.get()
                .uri("/roster/{team}/current", team)
                .retrieve()
                .body(RosterResponse.class);
        List<NhlRosterPlayer> players = new ArrayList<>();
        if (body != null) {
            if (body.forwards() != null) players.addAll(body.forwards());
            if (body.defensemen() != null) players.addAll(body.defensemen());
            if (body.goalies() != null) players.addAll(body.goalies());
        }
        return players;
    }
}
