package io.github.liamtkehoe.nhl_pickem.nhl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

public class NhlStatsClient {
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 50;
    private static final String SORT_BY_PLAYER_ID = "[{\"property\":\"playerId\",\"direction\":\"ASC\"}]";

    static final String ROOKIES_ONLY = " and isRookie='1'";

    private static final ParameterizedTypeReference<StatsPage<SkaterSummary>> SKATERS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<StatsPage<SkaterRealtime>> REALTIME = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<StatsPage<GoalieSummary>> GOALIES = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<StatsPage<SkaterPenaltyKill>> PENALTY_KILL = new ParameterizedTypeReference<>() {};

    private final RestClient http;

    public NhlStatsClient(RestClient http) {
        this.http = http;
    }

    public List<SkaterSummary> skaters(int seasonId) {
        return fetchAll("/skater/summary", regularSeason(seasonId), SKATERS);
    }

    public List<SkaterRealtime> skaterRealtime(int seasonId) {
        return fetchAll("/skater/realtime", regularSeason(seasonId), REALTIME);
    }

    public List<GoalieSummary> goalies(int seasonId) {
        return fetchAll("/goalie/summary", regularSeason(seasonId), GOALIES);
    }

    public List<SkaterPenaltyKill> penaltyKill(int seasonId) {
        return fetchAll("/skater/penaltykill", regularSeason(seasonId), PENALTY_KILL);
    }

    public Set<Long> rookieIds(int seasonId) {
        String filter = regularSeason(seasonId) + ROOKIES_ONLY;
        Set<Long> ids = new HashSet<>();
        fetchAll("/skater/summary", filter, SKATERS).forEach(s -> ids.add(s.playerId()));
        fetchAll("/goalie/summary", filter, GOALIES).forEach(g -> ids.add(g.playerId()));

        return ids;
    }

    private static String regularSeason(int seasonId) {
        return "seasonId=" + seasonId + " and gameTypeId=2";
    }

    private <T> List<T> fetchAll(String report, String filter, ParameterizedTypeReference<StatsPage<T>> type) {
        List<T> rows = new ArrayList<>();
        for (int page = 0; page < MAX_PAGES; page++) {
            int start = page * PAGE_SIZE;
            StatsPage<T> body = http.get()
                    .uri(builder -> builder.path(report)
                            .queryParam("start", start)
                            .queryParam("limit", PAGE_SIZE)
                            .queryParam("sort", "{sort}")
                            .queryParam("cayenneExp", "{filter}")
                            .build(SORT_BY_PLAYER_ID, filter))
                    .retrieve()
                    .body(type);
            if (body == null || body.data() == null || body.data().isEmpty()) {
                break;
            }
            rows.addAll(body.data());
            if (rows.size() >= body.total()) {
                break;
            }
        }

        return rows;
    }
}
