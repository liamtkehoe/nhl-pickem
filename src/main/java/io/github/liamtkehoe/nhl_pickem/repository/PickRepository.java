package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class PickRepository {
    private final JdbcClient jdbc;

    public PickRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Instant> submittedAt(long userId, int seasonId) {
        return jdbc.sql("SELECT submitted_at FROM submissions\n" +
                        "WHERE user_id = :u AND season_id = :s")
                .param("u", userId)
                .param("s", seasonId)
                .query(String.class)
                .optional()
                .map(Db::instant);
    }

    //Writes the submission row and every pick. Call inside a transaction
    public void insertAll(long userId, int seasonId, PickSet picks, Instant submittedAt) {
        jdbc.sql("INSERT INTO submissions (user_id, season_id, submitted_at) VALUES (:u, :s, :at)")
                .param("u", userId)
                .param("s", seasonId)
                .param("at", Db.text(submittedAt))
                .update();
        insertPicks(userId, seasonId, picks);
    }

    private void insertPicks(long userId, int seasonId, PickSet picks) {
        for (StandingsPick pick : picks.standings()) {
            jdbc.sql("INSERT INTO standings_picks (user_id, season_id, team, predicted_place) "
                            + "VALUES (:u, :s, :team, :place)")
                    .param("u", userId)
                    .param("s", seasonId)
                    .param("team", pick.team())
                    .param("place", pick.predictedPlace())
                    .update();
        }

        for (OuPick pick : picks.overUnders()) {
            jdbc.sql("INSERT INTO ou_picks (user_id, season_id, team, pick) VALUES (:u, :s, :team, :pick)")
                    .param("u", userId)
                    .param("s", seasonId)
                    .param("team", pick.team())
                    .param("pick", pick.pick().name())
                    .update();
        }

        for (AwardPick pick : picks.awards()) {
            jdbc.sql("INSERT INTO award_picks (user_id, season_id, award, player_id, team, pick_name) "
                            + "VALUES (:u, :s, :award, :playerId, :team, :name)")
                    .param("u", userId)
                    .param("s", seasonId)
                    .param("award", pick.award().name())
                    .param("playerId", pick.playerId())
                    .param("team", pick.team())
                    .param("name", pick.pickName())
                    .update();
        }
    }

    public PickSet find(long userId, int seasonId) {
        List<StandingsPick> standings = jdbc.sql(
                        "SELECT team, predicted_place FROM standings_picks\n" +
                                "WHERE user_id = :u\n" +
                                "AND season_id = :s\n" +
                                "ORDER BY team")
                .param("u", userId)
                .param("s", seasonId)
                .query((rs, row) -> new StandingsPick(rs.getString("team"), rs.getInt("predicted_place")))
                .list();

        List<OuPick> overUnders = jdbc.sql(
                        "SELECT team, pick FROM ou_picks\n" +
                                "WHERE user_id = :u\n" +
                                "AND season_id = :s\n" +
                                "ORDER BY team")
                .param("u", userId)
                .param("s", seasonId)
                .query((rs, row) -> new OuPick(rs.getString("team"), OverUnder.valueOf(rs.getString("pick"))))
                .list();

        List<AwardPick> awards = jdbc.sql(
                        "SELECT award, player_id, team, pick_name FROM award_picks\n" +
                                "WHERE user_id = :u\n" +
                                "AND season_id = :s\n" +
                                "ORDER BY award")
                .param("u", userId)
                .param("s", seasonId)
                .query((rs, row) -> {
                    long rawPlayerId = rs.getLong("player_id");
                    Long playerId = rs.wasNull() ? null : rawPlayerId;   // check right away, before reading any other column
                    return new AwardPick(
                            Award.valueOf(rs.getString("award")),
                            playerId,
                            rs.getString("team"),
                            rs.getString("pick_name"));
                })
                .list();

        return new PickSet(standings, overUnders, awards);
    }

    //List of total submissions in a season
    public List<Submission> submissions(int seasonId) {
        return jdbc.sql("SELECT s.user_id, u.username, s.submitted_at FROM submissions s\n" +
                        "JOIN users u ON u.id = s.user_id\n" +
                        "WHERE s.season_id = :s\n" +
                        "ORDER BY s.submitted_at")
                .param("s", seasonId)
                .query((rs, row) -> new Submission(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        Db.instant(rs.getString("submitted_at"))))
                .list();
    }

    //Removes every pick and submission a player has made, in every season
    public void deleteAllFor(long userId) {
        for (String table : List.of("award_picks", "ou_picks", "standings_picks", "submissions")) {
            jdbc.sql("DELETE FROM " + table + " WHERE user_id = :u").param("u", userId).update();
        }
    }

    //Swaps a player's picks for a season. The submission row, and its date, stay as they were
    public void replacePicks(long userId, int seasonId, PickSet picks) {
        for (String table : List.of("award_picks", "ou_picks", "standings_picks")) {
            jdbc.sql("DELETE FROM " + table + " WHERE user_id = :u AND season_id = :s")
                    .param("u", userId)
                    .param("s", seasonId)
                    .update();
        }
        insertPicks(userId, seasonId, picks);
    }
}
