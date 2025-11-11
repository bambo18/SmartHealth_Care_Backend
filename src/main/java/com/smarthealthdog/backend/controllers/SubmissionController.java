package com.smarthealthdog.backend.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthealthdog.backend.dto.diagnosis.get.SubmissionDetail;
import com.smarthealthdog.backend.dto.diagnosis.update.SubmissionResultRequest;
import com.smarthealthdog.backend.services.SubmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {
    private final SubmissionService submissionService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('can_update_health_records')")
    public ResponseEntity<Void> updateDiagnosis(
            @PathVariable("id") UUID submissionId,
            @Valid @RequestBody SubmissionResultRequest request
    ) {
        submissionService.completeDiagnosis(submissionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('can_view_own_health_records')")
    public ResponseEntity<SubmissionDetail> getSubmissionById(
            @PathVariable("id") UUID submissionId,
            @RequestParam(value = "languageCode", required = false) String languageCode,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(submissionService.getSubmissionAndDiagnosesById(submissionId, languageCode, userId));
    }
}