package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.domain.SubmissionStatus;
import com.smarthealthdog.backend.dto.diagnosis.update.DiagnosisResultDto;
import com.smarthealthdog.backend.dto.diagnosis.update.SubmissionResultRequest;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.SubmissionRepository;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceUT {
    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private DiagnosisService diagnosisService;

    @Test
    void completeDiagnosis_ShouldThrowInvalidRequestDataException_WhenSubmissionStatusIsNotProcessing() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.COMPLETED);
        when(submissionRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(mockSubmission));

        assertThrows(InvalidRequestDataException.class, () -> {
            submissionService.completeDiagnosis(UUID.randomUUID(), null);
        });
    }

    @Test
    void completeDiagnosis_ShouldThrowInvalidRequestDataException_WhenRequestIsNull() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.PROCESSING);
        when(submissionRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(mockSubmission));

        assertThrows(InvalidRequestDataException.class, () -> {
            submissionService.completeDiagnosis(UUID.randomUUID(), null);
        });
    }

    @Test
    void completeDiagnosis_ShouldThrowInvalidRequestDataException_WhenResultsAreEmpty() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.PROCESSING);
        when(submissionRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(mockSubmission));

        assertThrows(InvalidRequestDataException.class, () -> {
            submissionService.completeDiagnosis(UUID.randomUUID(), new SubmissionResultRequest());
        });
    }

    @Test
    void completeDiagnosis_ShouldThrowInvalidRequestDataException_WhenResultsAreNull() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.PROCESSING);
        when(submissionRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(mockSubmission));

        SubmissionResultRequest request = new SubmissionResultRequest();
        request.setResults(null);

        assertThrows(InvalidRequestDataException.class, () -> {
            submissionService.completeDiagnosis(UUID.randomUUID(), request);
        });
    }

    @Test
    void completeDiagnosis_ShouldProcessSuccessfully_WhenValidRequest() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.PROCESSING);
        when(submissionRepository.findById(any(UUID.class))).thenReturn(java.util.Optional.of(mockSubmission));
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);

        SubmissionResultRequest request = new SubmissionResultRequest();
        request.setResults(List.of(new DiagnosisResultDto()));

        submissionService.completeDiagnosis(UUID.randomUUID(), request);
        verify(submissionRepository).save(mockSubmission);
    }

    @Test
    void failSubmission_ShouldThrowInvalidRequestDataException_WhenSubmissionStatusIsNotProcessing() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.COMPLETED);

        assertThrows(InvalidRequestDataException.class, () -> {
            submissionService.failSubmission(mockSubmission, "Some failure reason");
        });
    }

    @Test
    void failSubmission_ShouldProcessSuccessfully_WhenValidSubmission() {
        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getStatus()).thenReturn(SubmissionStatus.PROCESSING);
        when(submissionRepository.save(any(Submission.class))).thenReturn(mockSubmission);

        submissionService.failSubmission(mockSubmission, "Some failure reason");
        verify(submissionRepository).save(mockSubmission);
    }
}
