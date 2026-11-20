package io.github.liamtkehoe.nhl_pickem.nhl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SkaterRealtime(long playerId, Integer giveaways, Integer takeaways, Integer blockedShots) {
}
