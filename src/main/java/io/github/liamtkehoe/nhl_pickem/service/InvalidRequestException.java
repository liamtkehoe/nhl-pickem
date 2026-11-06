package io.github.liamtkehoe.nhl_pickem.service;

import java.util.List;

public class InvalidRequestException extends RuntimeException {
    private final List<String> problems;

    public InvalidRequestException(List<String> problems) {
        super(String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> problems() {
        return problems;
    }
}
