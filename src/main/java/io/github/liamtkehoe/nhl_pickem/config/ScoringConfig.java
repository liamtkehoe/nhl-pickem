package io.github.liamtkehoe.nhl_pickem.config;

import io.github.liamtkehoe.nhl_pickem.scoring.ScoringEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ScoringConfig {
    @Bean
    public ScoringEngine scoringEngine() {
        return new ScoringEngine();
    }
}
