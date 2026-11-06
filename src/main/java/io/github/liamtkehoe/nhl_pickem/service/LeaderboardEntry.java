package io.github.liamtkehoe.nhl_pickem.service;

import java.time.Instant;

public record LeaderboardEntry(int rank,
                               String username,
                               int total,
                               int standingsPoints,
                               int overUnderPoints,
                               int awardPoints,
                               int exactPlaces,
                               int correctOverUnders,
                               Instant submittedAt) {
}
