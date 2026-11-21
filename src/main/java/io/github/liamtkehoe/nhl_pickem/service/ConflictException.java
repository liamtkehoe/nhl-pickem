package io.github.liamtkehoe.nhl_pickem.service;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
