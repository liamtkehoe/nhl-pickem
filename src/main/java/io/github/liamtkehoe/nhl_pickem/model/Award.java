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

    //Jack Adams is picked as a team; every other award as a player*/
    public boolean isTeamPick() {
        return this == JACK_ADAMS;
    }

    //Returns whether a player at this position can be picked. Positions: C, L, R, D, or G
    public boolean allowsPosition(String position) {
        return switch (this) {
            case VEZINA -> position.equals("G");
            case NORRIS -> position.equals("D");
            case SELKE -> position.equals("C") || position.equals("L") || position.equals("R");
            case ART_ROSS, ROCKET_RICHARD -> !position.equals("G");
            case HART, CALDER -> true;   // Calder's rookie check arrives with player stats in Phase 7
            case JACK_ADAMS -> false;    // a team pick, not a player
        };
    }
}
