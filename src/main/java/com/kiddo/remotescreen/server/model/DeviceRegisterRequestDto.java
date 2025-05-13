package com.kiddo.remotescreen.server.model;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegisterRequestDto(
        String password,
        String deviceName
) {}

