package io.github.liamtkehoe.nhl_pickem.scoring;

//actualPlace is null when the team isn't in the standings yet
public record StandingsResult(String team, int predictedPlace, Integer actualPlace, int points) {

}
