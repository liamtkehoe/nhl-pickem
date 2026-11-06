package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.model.User;
import io.github.liamtkehoe.nhl_pickem.repository.OULinesRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SeasonRepository;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import io.github.liamtkehoe.nhl_pickem.service.PickService;
import io.github.liamtkehoe.nhl_pickem.service.SubmitPicksRequest;
import io.github.liamtkehoe.nhl_pickem.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class PicksController {
    public static final String USER_HEADER = "X-Pickem-User";
    private static final List<String> POSITIONS = List.of("C", "L", "R", "D", "G");

    private final UserService users;
    private final PickService pickService;
    private final SnapshotRepository snapshots;
    private final OULinesRepository lines;
    private final SeasonRepository seasons;
    private final Clock clock;
    private final int season;

    public PicksController(UserService users, PickService pickService, SnapshotRepository snapshots, OULinesRepository lines, SeasonRepository seasons, Clock clock, @Value("${pickem.current-season}") int season) {
        this.users = users;
        this.pickService = pickService;
        this.snapshots = snapshots;
        this.lines = lines;
        this.seasons = seasons;
        this.clock = clock;
        this.season = season;
    }

    public record TeamOption(String team, String teamName, int currentPlace, int points, int gamesPlayed, Double line) {}
    public record DivisionOption(String division, List<TeamOption> teams) {}
    public record AwardOption(Award award, String displayName, boolean teamPick, List<String> positions) {}
    public record PickOptions(int season, boolean open, List<DivisionOption> divisions, List<AwardOption> awards, List<PlayerInfo> players) {}
    public record SubmitResponse(Instant submittedAt) {}

    @GetMapping("/pick-options")
    public PickOptions options(@RequestHeader(USER_HEADER) String username) {
        users.require(username);

        Map<String, Double> lineByTeam = lines.find(season);
        Map<String, List<TeamOption>> byDivision = new LinkedHashMap<>();
        for (TeamStanding t : snapshots.teams(season)) {
            byDivision.computeIfAbsent(t.division(), d -> new ArrayList<>())
                    .add(new TeamOption(t.team(), t.teamName(), t.place(), t.points(),
                            t.gamesPlayed(), lineByTeam.get(t.team())));
        }
        List<DivisionOption> divisions = byDivision.entrySet().stream()
                .map(e -> new DivisionOption(e.getKey(), e.getValue()))
                .toList();

        List<AwardOption> awards = Arrays.stream(Award.values())
                .map(a -> new AwardOption(a, a.displayName(), a.isTeamPick(),
                        POSITIONS.stream().filter(a::allowsPosition).toList()))
                .toList();

        boolean open = seasons.find(season).map(s -> s.isOpen(clock.instant())).orElse(false);

        return new PickOptions(season, open, divisions, awards, snapshots.players(season));
    }

    @PostMapping("/picks")
    public SubmitResponse submit(@RequestHeader(USER_HEADER) String username, @RequestBody SubmitPicksRequest request) {
        User user = users.require(username);

        return new SubmitResponse(pickService.submit(user.id(), season, request));
    }
}
