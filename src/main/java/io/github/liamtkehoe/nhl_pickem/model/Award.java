package io.github.liamtkehoe.nhl_pickem.model;

public enum Award {
    HART("Hart Memorial Trophy"),
    CALDER("Calder Memorial Trophy"),
    VEZINA("Vezina Trophy"),
    NORRIS("James Norris Memorial Trophy"),
    ART_ROSS("Art Ross Trophy"),
    SELKE("Frank J. Selke Trophy"),
    JACK_ADAMS("Jack Adams Award"),
    ROCKET_RICHARD("Maurice Richard Trophy");

    private final String displayName;

    Award(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** Jack Adams is picked as a team; every other award as a player*/
    public boolean isTeamPick() {
        return this == JACK_ADAMS;
    }
}
