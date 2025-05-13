package com.kiddo.remotescreen.server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LogoutUserDto(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Invalid email format")
        String email
) {

}
