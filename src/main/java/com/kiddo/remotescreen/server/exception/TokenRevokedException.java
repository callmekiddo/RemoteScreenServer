package com.kiddo.remotescreen.server.exception;

public class TokenRevokedException extends RuntimeException {

    public TokenRevokedException(String message) {
        super(message);
    }
}
