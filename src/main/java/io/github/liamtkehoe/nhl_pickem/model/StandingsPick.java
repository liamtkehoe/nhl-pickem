package io.github.liamtkehoe.nhl_pickem.model;

public record StandingsPick(String team, int predictedPlace) {
    public StandingsPick {
        if (predictedPlace < 1 || predictedPlace > 8) {
            throw new IllegalArgumentException("Place must be 1-8, got " + predictedPlace);
        }
    }
}
