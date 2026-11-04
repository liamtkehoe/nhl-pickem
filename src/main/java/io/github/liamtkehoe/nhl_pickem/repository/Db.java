package io.github.liamtkehoe.nhl_pickem.repository;

import java.time.Instant;

final class Db {
    private Db() {}

    static String text(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    static Instant instant(String text) {
        return text == null ? null : Instant.parse(text);
    }
}
