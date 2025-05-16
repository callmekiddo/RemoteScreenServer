package com.kiddo.remotescreen.server.model;

public record DeviceStatusDto(
        boolean allowRemote,
        String connectedAndroid
) {}
