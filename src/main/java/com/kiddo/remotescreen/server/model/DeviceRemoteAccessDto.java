package com.kiddo.remotescreen.server.model;

public record DeviceRemoteAccessDto(
        String deviceId,
        boolean allowRemote
) {}
