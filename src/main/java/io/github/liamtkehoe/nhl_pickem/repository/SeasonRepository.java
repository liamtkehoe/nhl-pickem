package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.Season;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SeasonRepository {
    private final JdbcClient jdbc;

    public SeasonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Inserts the season, or updates it if it already exists. */
    public void save(Season season) {
        jdbc.sql("INSERT INTO seasons (id, is_practice, picks_close_at) VALUES (:id, :practice, :closeAt)\n"
                        + "ON CONFLICT(id)\n" +
                        "DO UPDATE SET is_practice = excluded.is_practice, picks_close_at = excluded.picks_close_at")
                .param("id", season.id())
                .param("practice", season.practice() ? 1 : 0)
                .param("closeAt", Db.text(season.picksCloseAt()))
                .update();
    }

    public Optional<Season> find(int id) {
        return jdbc.sql("SELECT id, is_practice, picks_close_at FROM seasons\n" +
                        "WHERE id = :id")
                .param("id", id)
                .query((rs, row) -> new Season(
                        rs.getInt("id"),
                        rs.getInt("is_practice") == 1,
                        Db.instant(rs.getString("picks_close_at"))))
                .optional();
    }
}
