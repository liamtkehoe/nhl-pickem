package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.Award;
import io.github.liamtkehoe.nhl_pickem.scoring.AwardLeader;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AwardResultsRepository {
    private final JdbcClient jdbc;

    public AwardResultsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void saveFinal(int seasonId, Award award, Long playerId, String team, String source, Instant at) {
        jdbc.sql("INSERT INTO award_results (season_id, award, player_id, team, status, source, updated_at) "
                        + "VALUES (:s, :award, :playerId, :team, 'FINAL', :source, :at) "
                        + "ON CONFLICT(season_id, award) DO UPDATE SET player_id = excluded.player_id, "
                        + "team = excluded.team, status = 'FINAL', source = excluded.source, "
                        + "updated_at = excluded.updated_at")
                .param("s", seasonId)
                .param("award", award.name())
                .param("playerId", playerId)
                .param("team", team)
                .param("source", source)
                .param("at", Db.text(at))
                .update();
    }

    public Map<Award, AwardLeader> finals(int seasonId) {
        Map<Award, AwardLeader> finals = new EnumMap<>(Award.class);
        jdbc.sql("SELECT r.award, r.player_id, r.team, COALESCE(ps.name, pl.name, t.team_name, r.team) AS name FROM award_results r\n" +
                        "LEFT JOIN player_stats ps ON ps.season_id = r.season_id AND ps.player_id = r.player_id\n" +
                        "LEFT JOIN player_snapshot pl ON pl.season_id = r.season_id AND pl.player_id = r.player_id\n" +
                        "LEFT JOIN team_snapshot t ON t.season_id = r.season_id AND t.team = r.team\n" +
                        "WHERE r.season_id = :s\n" +
                        "AND r.status = 'FINAL'")
                .param("s", seasonId)
                .query((rs, row) -> {
                    Award award = Award.valueOf(rs.getString("award"));
                    long id = rs.getLong("player_id");
                    Long playerId = rs.wasNull() ? null : id;

                    return new AwardLeader(award, playerId, rs.getString("team"), rs.getString("name"), true);
                })
                .list()
                .forEach(leader -> finals.put(leader.award(), leader));

        return finals;
    }
}
