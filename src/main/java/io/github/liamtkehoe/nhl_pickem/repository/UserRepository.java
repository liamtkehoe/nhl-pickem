package io.github.liamtkehoe.nhl_pickem.repository;

import io.github.liamtkehoe.nhl_pickem.model.User;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public User create(String username, Instant createdAt) {
        jdbc.sql("INSERT INTO users (username, created_at) VALUES (:username, :createdAt)")
                .param("username", username)
                .param("createdAt", Db.text(createdAt))
                .update();
        return findByUsername(username).orElseThrow();
    }

    public Optional<User> findByUsername(String username) {
        return jdbc.sql("SELECT id, username, created_at FROM users\n" +
                        "WHERE username = :username")
                .param("username", username)
                .query((rs, row) -> new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        Db.instant(rs.getString("created_at"))))
                .optional();
    }

    public List<User> findAll() {
        return jdbc.sql("SELECT id, username, created_at FROM users\n" +
                        "ORDER BY username COLLATE NOCASE")
                .query((rs, row) -> new User(
                        rs.getLong("id"),
                        rs.getString("username"),
                        Db.instant(rs.getString("created_at"))))
                .list();
    }

    public void delete(long userId) {
        jdbc.sql("DELETE FROM users WHERE id = :id").param("id", userId).update();
    }
}
