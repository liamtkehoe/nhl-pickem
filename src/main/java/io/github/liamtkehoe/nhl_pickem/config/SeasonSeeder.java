package io.github.liamtkehoe.nhl_pickem.config;

import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeasonSeeder implements ApplicationRunner {
    public static final int PRACTICE_SEASON = 20262027;

    private final SeasonRepository seasons;

    public SeasonSeeder(SeasonRepository seasons) {
        this.seasons = seasons;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seasons.find(PRACTICE_SEASON).isEmpty()) {
            seasons.save(new Season(PRACTICE_SEASON, true, null));
        }
    }
}
