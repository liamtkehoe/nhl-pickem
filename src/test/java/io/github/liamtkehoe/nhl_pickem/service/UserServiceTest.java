package io.github.liamtkehoe.nhl_pickem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.liamtkehoe.nhl_pickem.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:target/test-pickem.db",
        "pickem.sync.enabled=false"
})
@Transactional
class UserServiceTest {
    @Autowired UserService users;

    @Test
    void signUpCreatesAnAccountWithTheTrimmedName() {
        assertThat(users.signUp("  newplayer ").username()).isEqualTo("newplayer");
    }

    @Test
    void signUpRefusesATakenUsernameInAnyCapitalization() {
        users.signUp("taken");

        assertThatThrownBy(() -> users.signUp("TAKEN"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is already taken");
    }

    @Test
    void signUpRejectsAnInvalidUsername() {
        assertThatThrownBy(() -> users.signUp("no spaces allowed"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void logInFindsAnExistingAccountInAnyCapitalization() {
        User created = users.signUp("returning");

        assertThat(users.logIn("Returning").id()).isEqualTo(created.id());
    }

    @Test
    void logInNeverCreatesAnAccount() {
        assertThatThrownBy(() -> users.logIn("typoed"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Please check the spelling");
        assertThatThrownBy(() -> users.logIn("typoed"))
                .isInstanceOf(NotFoundException.class);
    }
}
