package io.github.liamtkehoe.nhl_pickem.model;

public enum Award {
    HART("Hart Memorial Trophy", "The player judged most valuable to his team. Voted by the Professional Hockey Writers' Association."),
    CALDER("Calder Memorial Trophy", "The most proficient player in his first year of NHL competition. Voted by the Professional Hockey Writers' Association."),
    VEZINA("Vezina Trophy", "The goaltender judged best at his position. Voted by the NHL's general managers."),
    NORRIS("James Norris Memorial Trophy", "The defenseman who shows the greatest all-round ability at the position. Voted by the Professional Hockey Writers' Association."),
    ART_ROSS("Art Ross Trophy", "The player with the most points (goals plus assists) at the end of the regular season."),
    SELKE("Frank J. Selke Trophy", "The forward who best excels in the defensive aspects of the game. Voted by the Professional Hockey Writers' Association."),
    JACK_ADAMS("Jack Adams Award", "The coach judged to have contributed most to his team's success. Voted by the NHL Broadcasters' Association."),
    ROCKET_RICHARD("Maurice Richard Trophy", "The player who scores the most goals in the regular season.");

    private final String displayName;
    private final String description;

    Award(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    //Jack Adams is picked as a team; every other award as a player
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
            case HART, CALDER -> true;
            case JACK_ADAMS -> false; //team pick, not a player
        };
    }
}
