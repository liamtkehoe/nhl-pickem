package io.github.liamtkehoe.nhl_pickem.config;

import io.github.liamtkehoe.nhl_pickem.nhl.NhlApiClient;
import java.time.Duration;

import io.github.liamtkehoe.nhl_pickem.nhl.NhlStatsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NhlApiConfig {
    @Bean
    public NhlApiClient nhlApiClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        RestClient restClient = RestClient.builder()
                .baseUrl("https://api-web.nhle.com/v1")
                .defaultHeader(HttpHeaders.USER_AGENT, "nhl-pickem (personal project)")
                .requestFactory(requestFactory)
                .build();

        return new NhlApiClient(restClient);
    }

    @Bean
    public NhlStatsClient nhlStatsClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        RestClient restClient = RestClient.builder()
                .baseUrl("https://api.nhle.com/stats/rest/en")
                .defaultHeader(HttpHeaders.USER_AGENT, "nhl-pickem (personal project)")
                .requestFactory(requestFactory)
                .build();

        return new NhlStatsClient(restClient);
    }
}
