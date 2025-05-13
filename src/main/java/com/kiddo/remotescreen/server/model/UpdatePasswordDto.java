package com.kiddo.remotescreen.server.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordDto(
        @NotBlank(message = "Email must not be empty")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "New password must not be empty")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword,

        @NotBlank(message = "Confirm password must not be empty")
        @Size(min = 8, message = "Confirm password must be at least 8 characters")
        String confirmPassword
) {

}
