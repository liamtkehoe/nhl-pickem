package io.github.liamtkehoe.nhl_pickem.service;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
