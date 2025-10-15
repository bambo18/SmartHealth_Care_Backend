package com.smarthealthdog.backend.dto.walk;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record CreateWalkRequest(
        @NotNull OffsetDateTime start_time // "2025-09-22T09:30:00Z"
) {}