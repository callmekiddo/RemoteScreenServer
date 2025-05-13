package com.kiddo.remotescreen.server.model;

public record DeviceAuthRequestDto(
        String deviceId,
        String password
) {
}
