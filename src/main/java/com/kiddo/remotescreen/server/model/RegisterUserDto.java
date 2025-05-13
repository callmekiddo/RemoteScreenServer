package com.kiddo.remotescreen.server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserDto(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password must not be empty")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Full name must not be empty")
        String fullName
) {

}
