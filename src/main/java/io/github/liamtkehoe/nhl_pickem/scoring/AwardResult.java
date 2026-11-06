package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.Award;

//leaderName is null when the award has no leader yet
public record AwardResult(Award award, String pickName, String leaderName, boolean isFinal, int points) {
}
