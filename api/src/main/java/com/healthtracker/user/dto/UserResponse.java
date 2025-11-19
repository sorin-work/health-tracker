package com.healthtracker.user.dto;

import com.healthtracker.user.User;
import java.time.Instant;

public record UserResponse(Long id, String email, String displayName, String timezone, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getTimezone(), user.getCreatedAt());
    }
}
