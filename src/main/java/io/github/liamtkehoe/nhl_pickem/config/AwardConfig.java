package io.github.liamtkehoe.nhl_pickem.config;

import io.github.liamtkehoe.nhl_pickem.scoring.AwardFormulas;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardWeights;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AwardWeights.class)
public class AwardConfig {
    @Bean
    public AwardFormulas awardFormulas(AwardWeights weights) {
        return new AwardFormulas(weights);
    }
}
