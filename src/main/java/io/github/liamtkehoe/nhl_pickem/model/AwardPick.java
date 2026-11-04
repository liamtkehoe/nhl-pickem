package io.github.liamtkehoe.nhl_pickem.model;

public record AwardPick(Award award, Long playerId, String team, String pickName) {
    public AwardPick {
        if (award.isTeamPick() && (team == null || playerId != null)) {
            throw new IllegalArgumentException(award + " must be picked as a team");
        }
        if (!award.isTeamPick() && (playerId == null || team != null)) {
            throw new IllegalArgumentException(award + " must be picked as a player");
        }
    }

    public static AwardPick player(Award award, long playerId, String playerName) {
        return new AwardPick(award, playerId, null, playerName);
    }

    public static AwardPick team(Award award, String team, String coachLabel) {
        return new AwardPick(award, null, team, coachLabel);
    }
}
