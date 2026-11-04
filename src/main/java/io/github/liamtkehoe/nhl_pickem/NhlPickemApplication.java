package io.github.liamtkehoe.nhl_pickem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class NhlPickemApplication {
	static void main(String[] args) throws IOException {
		Files.createDirectories(Path.of("data"));
		SpringApplication.run(NhlPickemApplication.class, args);
	}
}
