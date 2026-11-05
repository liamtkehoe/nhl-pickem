package io.github.liamtkehoe.nhl_pickem.scoring;

import io.github.liamtkehoe.nhl_pickem.model.Award;

public record AwardLeader(Award award, Long playerId, String team, String name, boolean isFinal) {
}
