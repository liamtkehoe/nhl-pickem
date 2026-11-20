package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.PlayerStats;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StatsRepository {
    private final JdbcClient jdbc;

    public StatsRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public void replaceAll(int seasonId, List<PlayerStats> rows, Instant fetchedAt) {
        jdbc.sql("DELETE FROM player_stats WHERE season_id = :s").param("s", seasonId).update();
        for (PlayerStats p : rows) {
            jdbc.sql("INSERT INTO player_stats (season_id, player_id, name, team, position, is_rookie, "
                            + "games_played, goals, assists, points, plus_minus, giveaways, takeaways, "
                            + "wins, save_pct, gaa, fetched_at) VALUES (:s, :id, :name, :team, :position, "
                            + ":rookie, :gp, :goals, :assists, :points, :pm, :give, :take, :wins, :sv, :gaa, :at)")
                    .param("s", seasonId)
                    .param("id", p.playerId())
                    .param("name", p.name())
                    .param("team", p.team())
                    .param("position", p.position())
                    .param("rookie", p.rookie() ? 1 : 0)
                    .param("gp", p.gamesPlayed())
                    .param("goals", p.goals())
                    .param("assists", p.assists())
                    .param("points", p.points())
                    .param("pm", p.plusMinus())
                    .param("give", p.giveaways())
                    .param("take", p.takeaways())
                    .param("wins", p.wins())
                    .param("sv", p.savePct())
                    .param("gaa", p.gaa())
                    .param("at", Db.text(fetchedAt))
                    .update();
        }
    }

    public List<PlayerStats> find(int seasonId) {
        return jdbc.sql("SELECT * FROM player_stats WHERE season_id = :s")
                .param("s", seasonId)
                .query((rs, row) -> {
                    double sv = rs.getDouble("save_pct");
                    Double savePct = rs.wasNull() ? null : sv;
                    double gaaValue = rs.getDouble("gaa");
                    Double gaa = rs.wasNull() ? null : gaaValue;
                    return new PlayerStats(
                            rs.getLong("player_id"),
                            rs.getString("name"),
                            rs.getString("team"),
                            rs.getString("position"),
                            rs.getInt("is_rookie") == 1,
                            rs.getInt("games_played"),
                            rs.getInt("goals"),
                            rs.getInt("assists"),
                            rs.getInt("points"),
                            rs.getInt("plus_minus"),
                            rs.getInt("giveaways"),
                            rs.getInt("takeaways"),
                            rs.getInt("wins"),
                            savePct,
                            gaa);
                })
                .list();
    }
}
