package com.smarthealthdog.backend.dto.walk;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWalkRequest(
        @NotBlank Instant startTime, // "2025-09-22T09:30:00Z"
        @NotBlank Instant endTime,
        @NotNull @DecimalMin(value = "0.00", inclusive = true) BigDecimal distanceKm,
        List<List<Double>> pathCoordinates  // [ [lat, lng], ... ]
) {}