package com.healthtracker.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters") String password,
        @NotBlank @Size(max = 100) String displayName,
        @Size(max = 64) String timezone) {}
