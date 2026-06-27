package com.healthtracker.user.dto;

import com.healthtracker.user.GlucoseUnit;
import com.healthtracker.user.UnitSystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Unit preferences are optional so older clients that only send name and zone keep working. */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 64) String timezone,
        UnitSystem unitSystem,
        GlucoseUnit glucoseUnit) {}
