package com.kiddo.remotescreen.server.model;

public record LoginResponse(
    String token,
    long expiresIn,

    String fullName
) {
}
