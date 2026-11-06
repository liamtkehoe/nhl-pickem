package io.github.liamtkehoe.nhl_pickem.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class OULinesRepository {
    private final JdbcClient jdbc;

    public OULinesRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public int count(int seasonId) {
        return jdbc.sql("SELECT COUNT(*) FROM ou_lines\n" +
                        "WHERE season_id = :s")
                .param("s", seasonId)
                .query(Integer.class)
                .single();
    }

    @Transactional
    public void saveAll(int seasonId, Map<String, Double> lines, String source, String asOf) {
        lines.forEach((team, line) -> jdbc.sql(
                        "INSERT INTO ou_lines (season_id, team, line, source, as_of)\n" +
                                "VALUES (:s, :team, :line, :source, :asOf)")
                .param("s", seasonId)
                .param("team", team)
                .param("line", line)
                .param("source", source)
                .param("asOf", asOf)
                .update());
    }

    public Map<String, Double> find(int seasonId) {
        return jdbc.sql("SELECT team, line FROM ou_lines\n" +
                        "WHERE season_id = :s")
                .param("s", seasonId)
                .query((rs, row) -> Map.entry(rs.getString("team"), rs.getDouble("line")))
                .list()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
