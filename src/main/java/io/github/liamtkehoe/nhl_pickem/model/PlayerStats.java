package io.github.liamtkehoe.nhl_pickem.model;

public record PlayerStats(
        long playerId,
        String name,
        String team,
        String position,
        boolean rookie,
        int gamesPlayed,
        int goals,
        int assists,
        int points,
        int plusMinus,
        int giveaways,
        int takeaways,
        int wins,
        Double savePct,
        Double gaa) {

    public boolean isGoalie() {
        return position.equalsIgnoreCase("G");
    }

    public boolean isForward() {
        return position.equalsIgnoreCase("C") || position.equalsIgnoreCase("L") || position.equalsIgnoreCase("R");
    }
}
