package io.github.liamtkehoe.nhl_pickem.config;

import io.github.liamtkehoe.nhl_pickem.model.Season;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeasonSeeder implements ApplicationRunner {
    public static final int PRACTICE_SEASON = 20262027;

    private final SeasonRepository seasons;
    private final int currentSeason;
    private final boolean practice;

    public SeasonSeeder(SeasonRepository seasons, @Value("${pickem.current-season}") int currentSeason, @Value("${pickem.current-season-practice:false}") boolean practice) {
        this.seasons = seasons;
        this.currentSeason = currentSeason;
        this.practice = practice;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seasons.find(currentSeason).isEmpty()) {
            seasons.save(new Season(PRACTICE_SEASON, practice, null));
        }
    }
}
