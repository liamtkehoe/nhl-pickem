package io.github.liamtkehoe.nhl_pickem;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:sqlite:target/test-pickem.db",
		"pickem.sync.enabled=false"
})
class NhlPickemApplicationTests {

	@Test
	void contextLoads() {
	}

}
