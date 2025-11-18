package com.smarthealthdog.backend.dto.diagnosis.get;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import lombok.Value;

@Value
public class SubmissionDetail {
    private final UUID id;
    private final String petId; // Assuming you only need the ID, not the full Pet object
    private final String photoUrl;
    private final String status; // String representation of the SubmissionStatus
    private final Instant submittedAt;
    private final Instant completedAt;
    private final String failureReason;
    
    // The collection of all diagnoses associated with this submission
    private final Set<DiagnosisResult> diagnoses;
}
