package io.github.liamtkehoe.nhl_pickem.model;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class SeasonTest {
    @Test
    void labelUsesTheShortEndYear() {
        assertThat(new Season(20262027, false, null).label()).isEqualTo("2026–27");
    }

    @Test
    void labelKeepsTheLeadingZeroAcrossACentury() {
        assertThat(new Season(19992000, false, null).label()).isEqualTo("1999–00");
    }
}
