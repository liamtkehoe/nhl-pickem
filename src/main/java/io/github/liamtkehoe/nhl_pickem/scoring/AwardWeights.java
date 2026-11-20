package io.github.liamtkehoe.nhl_pickem.scoring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "pickem.awards")
public record AwardWeights(
        @DefaultValue("0.5") double minGamesShare,      //amount of team games a candidate must have played
        @DefaultValue("2.0") double goalWeight,         //Hart/Norris production = goalWeight x goals + assists
        @DefaultValue("1.0") double goaliePenalty,      //subtracted from goalies' Hart scores
        @DefaultValue("0.4") double mvpGoalieSavePct,
        @DefaultValue("0.3") double mvpGoalieGaa,
        @DefaultValue("0.2") double mvpGoalieWins,
        @DefaultValue("0.1") double mvpGoalieShutouts,
        @DefaultValue("1.5") double calderGoalWeight,       //Calder production = calderGoalWeight x goals + assists
        @DefaultValue("1.15") double calderDefenseMultiplier,
        @DefaultValue("0.5") double calderGoaliePenalty,
        @DefaultValue("0.6") double vezinaSavePct,
        @DefaultValue("0.25") double vezinaWins,
        @DefaultValue("0.15") double vezinaGaa,
        @DefaultValue("0.75") double norrisProduction,
        @DefaultValue("0.15") double norrisPlusMinus,
        @DefaultValue("0.10") double norrisTakeGive,
        @DefaultValue("90") double selkeMinPkSeconds,       //average PK time per game to be a Selke candidate
        @DefaultValue("0.5") double selkeMinPointsPerGame,
        @DefaultValue("0.25") double selkePlusMinus,
        @DefaultValue("0.15") double selkeTakeGive,
        @DefaultValue("0.15") double selkeBlocks,
        @DefaultValue("0.30") double selkePkTime,
        @DefaultValue("0.15") double selkePkGoalsAgainst) {

    public static AwardWeights defaults() {
        return new AwardWeights(0.5, 2.0, 1.0, 0.4, 0.3, 0.2, 0.1, 1.5, 1.15, 0.5,
                0.6, 0.25, 0.15, 0.75, 0.15, 0.10, 90, 0.5, 0.25, 0.15, 0.15, 0.30, 0.15);
    }
}
