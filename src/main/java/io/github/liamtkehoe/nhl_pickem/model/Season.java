package io.github.liamtkehoe.nhl_pickem.model;

import java.time.Instant;

public record Season(int id, boolean practice, Instant picksCloseAt) {
    public boolean isOpen(Instant now) {
        return picksCloseAt == null || now.isBefore(picksCloseAt);
    }

    public String label() {
        int startYear = id / 10000;
        int endYear = id % 100;

        return String.format("%d–%02d", startYear, endYear);
    }
}
