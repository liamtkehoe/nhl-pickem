package io.github.liamtkehoe.nhl_pickem.scoring;

import java.util.List;

public record PlayerScore(
        String username,
        int total,
        int standingsPoints,
        int overUnderPoints,
        int awardPoints,
        int exactPlaces,
        int correctOverUnders,
        List<StandingsResult> standings,
        List<OuResult> overUnders,
        List<AwardResult> awards) {

    public static PlayerScore of(String username, List<StandingsResult> standings, List<OuResult> overUnders, List<AwardResult> awards) {
        int standingsPoints = standings.stream().mapToInt(StandingsResult::points).sum();
        int overUnderPoints = overUnders.stream().mapToInt(OuResult::points).sum();
        int awardPoints = awards.stream().mapToInt(AwardResult::points).sum();
        int exactPlaces = (int) standings.stream()
                .filter(r -> r.points() == ScoringEngine.EXACT_PLACE)
                .count();
        int correctOverUnders = (int) overUnders.stream().filter(r -> r.points() > 0).count();

        return new PlayerScore(username,
                standingsPoints + overUnderPoints + awardPoints,
                standingsPoints, overUnderPoints, awardPoints,
                exactPlaces, correctOverUnders,
                standings, overUnders, awards);
    }
}
