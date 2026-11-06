package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.nhl.NhlApiClient;
import io.github.liamtkehoe.nhl_pickem.nhl.NhlRosterPlayer;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NhlSyncService {
    private static final Logger log = LoggerFactory.getLogger(NhlSyncService.class);

    private final NhlApiClient nhl;
    private final SnapshotRepository snapshots;
    private final Clock clock;
    private final int season;
    private final boolean enabled;

    public NhlSyncService(NhlApiClient nhl,
                          SnapshotRepository snapshots,
                          Clock clock,
                          @Value("${pickem.current-season}") int season,
                          @Value("${pickem.sync.enabled:true}") boolean enabled) {
        this.nhl = nhl;
        this.snapshots = snapshots;
        this.clock = clock;
        this.season = season;
        this.enabled = enabled;
    }

    //First sync, once the app has fully started
    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        if (!enabled) {
            log.info("NHL sync is turned off");
            return;
        }
        safely("Standings", this::syncStandings);
        safely("Rosters", this::syncRosters);
    }

    @Scheduled(initialDelay = 10, fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    public void scheduledStandings() {
        safely("Standings", this::syncStandings);
    }

    @Scheduled(initialDelay = 6, fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    public void scheduledRosters() {
        safely("Rosters", this::syncRosters);
    }

    public int syncStandings() {
        Instant now = clock.instant();
        List<TeamStanding> teams = nhl.standingsNow().stream()
                .map(s -> new TeamStanding(
                        s.teamAbbrev().value(),
                        s.teamName().value(),
                        s.divisionName(),
                        s.divisionSequence(),
                        s.points(),
                        s.gamesPlayed(),
                        now))
                .toList();
        if (teams.isEmpty()) {
            throw new IllegalStateException("NHL returned no standings");
        }
        snapshots.replaceTeams(season, teams);
        log.info("Standings synced: {} teams", teams.size());
        return teams.size();
    }

    public int syncRosters() {
        List<TeamStanding> teams = snapshots.teams(season);
        if (teams.isEmpty()) {
            syncStandings();
            teams = snapshots.teams(season);
        }

        Instant now = clock.instant();
        List<PlayerInfo> players = new ArrayList<>();
        for (TeamStanding team : teams) {
            try {
                for (NhlRosterPlayer p : nhl.currentRoster(team.team())) {
                    players.add(new PlayerInfo(p.id(), p.fullName(), team.team(), p.positionCode()));
                }
                Thread.sleep(250); // be polite: about 4 requests a second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                log.warn("Roster for {} failed: {}", team.team(), e.getMessage());
            }
        }

        snapshots.upsertPlayers(season, players, now);
        log.info("Rosters synced: {} players", players.size());
        return players.size();
    }

    private void safely(String what, IntSupplier task) {
        try {
            task.getAsInt();
        } catch (RuntimeException e) {
            log.warn("{} sync failed, keeping the last snapshot: {}", what, e.getMessage());
        }
    }
}
