package io.github.liamtkehoe.nhl_pickem.model;

import java.time.Instant;

public record Submission(long userId, String username, Instant submittedAt) {
}
