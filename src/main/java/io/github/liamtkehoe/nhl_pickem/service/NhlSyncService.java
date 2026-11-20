package io.github.liamtkehoe.nhl_pickem.service;

import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.PlayerStats;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import io.github.liamtkehoe.nhl_pickem.nhl.*;
import io.github.liamtkehoe.nhl_pickem.repository.SnapshotRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import io.github.liamtkehoe.nhl_pickem.repository.StatsRepository;
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
    private final StatsRepository statsRepository;
    private final NhlStatsClient stats;
    private final Clock clock;
    private final int season;
    private final boolean enabled;

    public NhlSyncService(NhlApiClient nhl,
                          SnapshotRepository snapshots,
                          StatsRepository statsRepository,
                          NhlStatsClient stats,
                          Clock clock,
                          @Value("${pickem.current-season}") int season,
                          @Value("${pickem.sync.enabled:true}") boolean enabled) {
        this.nhl = nhl;
        this.snapshots = snapshots;
        this.statsRepository = statsRepository;
        this.stats = stats;
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
        safely("Stats", this::syncStats);
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

    @Scheduled(initialDelay = 30, fixedDelay = 30, timeUnit = TimeUnit.MINUTES)
    public void scheduledStats() {
        safely("Stats", this::syncStats);
    }

    public int syncStats() {
        Instant now = clock.instant();

        Map<Long, String> rosterTeams = snapshots.players(season).stream()
                .collect(Collectors.toMap(PlayerInfo::playerId, PlayerInfo::team, (a, b) -> a));

        Map<Long, SkaterRealtime> realtime = stats.skaterRealtime(season).stream()
                .collect(Collectors.toMap(SkaterRealtime::playerId, Function.identity(), (a, b) -> a));

        Map<Long, SkaterPenaltyKill> penaltyKill;
        try {
            penaltyKill = stats.penaltyKill(season).stream()
                    .collect(Collectors.toMap(SkaterPenaltyKill::playerId, Function.identity(), (a, b) -> a));
        } catch (RuntimeException e) {
            log.warn("Penalty-kill lookup failed. No Selke candidates this sync: {}", e.getMessage());
            penaltyKill = Map.of();
        }

        Set<Long> rookies;
        try {
            rookies = stats.rookieIds(season);
        } catch (RuntimeException e) {
            log.warn("Rookie lookup failed. No Calder leader this sync: {}", e.getMessage());
            rookies = Set.of();
        }

        Map<Long, PlayerStats> rows = new LinkedHashMap<>();
        for (SkaterSummary s : stats.skaters(season)) {
            SkaterRealtime rt = realtime.get(s.playerId());
            SkaterPenaltyKill pk = penaltyKill.get(s.playerId());
            rows.putIfAbsent(s.playerId(), new PlayerStats(
                    s.playerId(),
                    s.skaterFullName(),
                    currentTeam(s.playerId(), s.teamAbbrevs(), rosterTeams),
                    s.positionCode(),
                    rookies.contains(s.playerId()),
                    s.gamesPlayed(), s.goals(), s.assists(), s.points(), s.plusMinus(),
                    rt == null ? 0 : orZero(rt.giveaways()),
                    rt == null ? 0 : orZero(rt.takeaways()),
                    rt == null ? 0 : orZero(rt.blockedShots()),
                    pk == null || pk.shTimeOnIcePerGame() == null ? 0 : pk.shTimeOnIcePerGame(),
                    pk == null ? 0 : orZero(pk.shGoalsAgainst()),
                    0, 0, null, null));
        }
        for (GoalieSummary g : stats.goalies(season)) {
            rows.putIfAbsent(g.playerId(), new PlayerStats(
                    g.playerId(),
                    g.goalieFullName(),
                    currentTeam(g.playerId(), g.teamAbbrevs(), rosterTeams),
                    "G",
                    rookies.contains(g.playerId()),
                    g.gamesPlayed(), 0, 0, 0, 0,
                    0, 0, 0, 0, 0,
                    g.wins(), orZero(g.shutouts()), g.savePct(), g.goalsAgainstAverage()));
        }

        statsRepository.replaceAll(season, List.copyOf(rows.values()), now);
        log.info("Stats synced: {} players ({} rookies, {} with PK time)", rows.size(), rookies.size(), penaltyKill.size());

        return rows.size();
    }

    private static int orZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String currentTeam(long playerId, String teamAbbrevs, Map<Long, String> rosterTeams) {
        String rosterTeam = rosterTeams.get(playerId);
        if (rosterTeam != null) {
            return rosterTeam;
        }
        if (teamAbbrevs == null || teamAbbrevs.isBlank()) {
            return "";
        }
        String[] teams = teamAbbrevs.split(",");

        return teams[teams.length - 1].trim();
    }
}
