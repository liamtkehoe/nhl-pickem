package io.github.liamtkehoe.nhl_pickem.model;

import java.time.Instant;

public record User(long id, String username, Instant createdAt) {
}
