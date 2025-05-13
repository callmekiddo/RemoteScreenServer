package com.kiddo.remotescreen.server.model;

public record DeviceUpdatePasswordDto(
        String deviceId,
        String newPassword
) {}
