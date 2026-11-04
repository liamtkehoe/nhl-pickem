package io.github.liamtkehoe.nhl_pickem.web;

import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import io.github.liamtkehoe.nhl_pickem.service.NhlSyncService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StandingsController {
    private final SnapshotRepository snapshots;
    private final NhlSyncService sync;
    private final int season;

    public StandingsController(SnapshotRepository snapshots,
                               NhlSyncService sync,
                               @Value("${pickem.current-season}") int season) {
        this.snapshots = snapshots;
        this.sync = sync;
        this.season = season;
    }

    /** {"Atlantic": [8 teams], "Metropolitan": [...], "Central": [...], "Pacific": [...]} */
    @GetMapping("/standings")
    public Map<String, List<TeamStanding>> standings() {
        return snapshots.teams(season).stream()
                .collect(Collectors.groupingBy(
                        TeamStanding::division, LinkedHashMap::new, Collectors.toList()));
    }

    public record RefreshResult(int teams, int players) {}

    @PostMapping("/admin/refresh")
    public RefreshResult refresh() {
        return new RefreshResult(sync.syncStandings(), sync.syncRosters());
    }
}
