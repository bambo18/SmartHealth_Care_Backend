package com.smarthealthdog.backend.dto.diagnosis.create;

import com.smarthealthdog.backend.domain.PetSpecies;

public record RequestDiagnosisData (
    String imageUrl,
    Long submissionId,
    PetSpecies species
) {}