package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.PlayerInfo;
import io.github.liamtkehoe.nhl_pickem.model.TeamStanding;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SnapshotRepository {
    private final JdbcClient jdbc;

    public SnapshotRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void replaceTeams(int seasonId, List<TeamStanding> teams) {
        jdbc.sql("DELETE FROM team_snapshot\n" +
                        "WHERE season_id = :s")
                .param("s", seasonId)
                .update();
        for (TeamStanding t : teams) {
            jdbc.sql("INSERT INTO team_snapshot "
                            + "(season_id, team, team_name, division, place, points, games_played, fetched_at) "
                            + "VALUES (:s, :team, :name, :division, :place, :points, :gp, :at)")
                    .param("s", seasonId)
                    .param("team", t.team())
                    .param("name", t.teamName())
                    .param("division", t.division())
                    .param("place", t.place())
                    .param("points", t.points())
                    .param("gp", t.gamesPlayed())
                    .param("at", Db.text(t.fetchedAt()))
                    .update();
        }
    }

    /** All teams, East divisions first, each division in standings order. */
    public List<TeamStanding> teams(int seasonId) {
        return jdbc.sql("SELECT team, team_name, division, place, points, games_played, fetched_at FROM team_snapshot\n" +
                        "WHERE season_id = :s\n"
                        + "ORDER BY CASE division WHEN 'Atlantic' THEN 1 WHEN 'Metropolitan' THEN 2 WHEN 'Central' THEN 3 ELSE 4 END, place")
                .param("s", seasonId)
                .query((rs, row) -> new TeamStanding(
                        rs.getString("team"),
                        rs.getString("team_name"),
                        rs.getString("division"),
                        rs.getInt("place"),
                        rs.getInt("points"),
                        rs.getInt("games_played"),
                        Db.instant(rs.getString("fetched_at"))))
                .list();
    }

    @Transactional
    public void upsertPlayers(int seasonId, List<PlayerInfo> players, Instant fetchedAt) {
        for (PlayerInfo p : players) {
            jdbc.sql("INSERT INTO player_snapshot (season_id, player_id, name, team, position, fetched_at) "
                            + "VALUES (:s, :id, :name, :team, :position, :at) "
                            + "ON CONFLICT(season_id, player_id) DO UPDATE SET "
                            + "name = excluded.name, team = excluded.team, "
                            + "position = excluded.position, fetched_at = excluded.fetched_at")
                    .param("s", seasonId)
                    .param("id", p.playerId())
                    .param("name", p.name())
                    .param("team", p.team())
                    .param("position", p.position())
                    .param("at", Db.text(fetchedAt))
                    .update();
        }
    }
}
