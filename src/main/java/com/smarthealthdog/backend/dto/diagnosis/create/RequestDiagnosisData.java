package com.smarthealthdog.backend.dto.diagnosis.create;

import java.util.UUID;

import com.smarthealthdog.backend.domain.PetSpecies;

public record RequestDiagnosisData (
    String imageUrl,
    UUID submissionId,
    PetSpecies species
) {}