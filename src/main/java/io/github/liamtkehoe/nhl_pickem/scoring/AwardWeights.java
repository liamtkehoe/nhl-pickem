package io.github.liamtkehoe.nhl_pickem.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pickem.awards")
public record AwardWeights(
        @DefaultValue("0.5") double minGamesShare,      //amount of team games a candidate must have played
        @DefaultValue("2.0") double goalWeight,         //production = goalWeight x goals + assists
        @DefaultValue("1.0") double goaliePenalty,      //subtracted from goalies' Hart/Calder scores
        @DefaultValue("0.6") double vezinaSavePct,
        @DefaultValue("0.25") double vezinaWins,
        @DefaultValue("0.15") double vezinaGaa,
        @DefaultValue("0.75") double norrisProduction,
        @DefaultValue("0.15") double norrisPlusMinus,
        @DefaultValue("0.10") double norrisTakeGive,
        @DefaultValue("0.7") double selkePlusMinus,
        @DefaultValue("0.3") double selkeTakeGive
) {
}
