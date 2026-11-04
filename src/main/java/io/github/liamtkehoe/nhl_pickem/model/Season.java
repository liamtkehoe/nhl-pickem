package io.github.liamtkehoe.nhl_pickem.model;

import java.time.Instant;

public record Season(int id, boolean practice, Instant picksCloseAt) {
    public boolean isOpen(Instant now) {
        return picksCloseAt == null || now.isBefore(picksCloseAt);
    }
}
