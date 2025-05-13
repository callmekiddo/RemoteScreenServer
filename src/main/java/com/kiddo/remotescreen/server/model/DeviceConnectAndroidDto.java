package com.kiddo.remotescreen.server.model;

public record DeviceConnectAndroidDto(
        String deviceId,
        String password,
        String androidDeviceName
) {}