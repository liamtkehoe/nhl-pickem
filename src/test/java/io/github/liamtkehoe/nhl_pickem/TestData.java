package io.github.liamtkehoe.nhl_pickem;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.OverUnder;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.AwardEntry;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.OuEntry;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest.StandingsEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TestData {
    private TestData() {}

    private static final List<String> DIVISIONS = List.of("Atlantic", "Metropolitan", "Central", "Pacific");
    private static final List<String> TEAMS = List.of(
            "ANA", "BOS", "BUF", "CGY", "CAR", "CHI", "COL", "CBJ",
            "DAL", "DET", "EDM", "FLA", "LAK", "MIN", "MTL", "NSH",
            "NJD", "NYI", "NYR", "OTT", "PHI", "PIT", "SJS", "SEA",
            "STL", "TBL", "TOR", "UTA", "VAN", "VGK", "WSH", "WPG");

    public static List<TeamStanding> teams() {
        List<TeamStanding> teams = new ArrayList<>();
        for (int i = 0; i < TEAMS.size(); i++) {
            String code = TEAMS.get(i);
            int place = i % 8 + 1;
            teams.add(new TeamStanding(code, code + " Club", DIVISIONS.get(i / 8), place, 21 - place, 12, Instant.EPOCH));
        }

        return teams;
    }

    public static List<PlayerInfo> players() {
        return List.of(
                new PlayerInfo(1, "Fake Center", "ANA", "C", true),
                new PlayerInfo(2, "Fake Defenseman", "BOS", "D", false),
                new PlayerInfo(3, "Fake Goalie", "BUF", "G", false));
    }

    public static Map<Long, PlayerInfo> playersById() {
        return players().stream().collect(Collectors.toMap(PlayerInfo::playerId, Function.identity()));
    }

    //Every team in its current place, every team OVER, eligible award picks
    public static SubmitPicksRequest perfectRequest() {
        return request(false);
    }

    //Every division in reverse order, every team UNDER
    public static SubmitPicksRequest backwardsRequest() {
        return request(true);
    }

    private static SubmitPicksRequest request(boolean backwards) {
        List<StandingsEntry> standings = teams().stream()
                .map(t -> new StandingsEntry(t.team(), backwards ? 9 - t.place() : t.place()))
                .toList();
        List<OuEntry> overUnders = teams().stream()
                .map(t -> new OuEntry(t.team(), backwards ? OverUnder.UNDER : OverUnder.OVER))
                .toList();

        List<AwardEntry> awards = new ArrayList<>();
        for (Award award : Award.values()) {
            if (award.isTeamPick()) {
                awards.add(new AwardEntry(award, null, "PHI"));
            } else {
                long playerId = award == Award.VEZINA ? 3 : award == Award.NORRIS ? 2 : 1;
                awards.add(new AwardEntry(award, playerId, null));
            }
        }

        return new SubmitPicksRequest(standings, overUnders, awards);
    }
}
