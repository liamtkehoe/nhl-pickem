package io.github.liamtkehoe.nhl_pickem.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "pickem.sync.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
