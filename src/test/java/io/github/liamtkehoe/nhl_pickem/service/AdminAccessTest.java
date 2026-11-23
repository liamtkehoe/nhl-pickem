package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AdminAccessTest {

    @Test
    void matchesConfiguredNamesInAnyCapitalization() {
        AdminAccess access = new AdminAccess(List.of(" LiamKehoe ", "Other"));

        assertThat(access.isAdmin("liamkehoe")).isTrue();
        assertThat(access.isAdmin("OTHER")).isTrue();
        assertThat(access.isAdmin("someoneelse")).isFalse();
    }

    @Test
    void nobodyIsAnAdminWhenNoneAreConfigured() {
        AdminAccess access = new AdminAccess(List.of(""));

        assertThat(access.isAdmin("LiamKehoe")).isFalse();
        assertThat(access.isAdmin(null)).isFalse();
    }
}
