package io.github.liamtkehoe.nhl_pickem.service;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
