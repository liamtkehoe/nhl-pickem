package io.github.liamtkehoe.nhl_pickem.nhl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class NhlStatsClientTest {
    private static final String BASE = "https://api.nhle.com/stats/rest/en";

    @Test
    void readsEveryPageUntilTheTotalIsReached() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        NhlStatsClient client = new NhlStatsClient(builder.build());

        server.expect(requestTo(startsWith(BASE + "/skater/realtime")))
                .andExpect(queryParam("start", "0"))
                .andRespond(withSuccess(page(1, 100, 101), MediaType.APPLICATION_JSON));
        server.expect(requestTo(startsWith(BASE + "/skater/realtime")))
                .andExpect(queryParam("start", "100"))
                .andRespond(withSuccess(page(101, 1, 101), MediaType.APPLICATION_JSON));

        List<SkaterRealtime> rows = client.skaterRealtime(20262027);

        assertThat(rows).hasSize(101);
        assertThat(rows.getLast().playerId()).isEqualTo(101);
        server.verify();   // fails if a third page was requested, or the second never was
    }

    private static String page(int firstId, int count, int total) {
        String rows = IntStream.range(firstId, firstId + count)
                .mapToObj(id -> "{\"playerId\":" + id + ",\"giveaways\":1,\"takeaways\":2}")
                .collect(Collectors.joining(","));

        return "{\"data\":[" + rows + "],\"total\":" + total + "}";
    }
}
