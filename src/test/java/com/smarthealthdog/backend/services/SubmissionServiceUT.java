package com.smarthealthdog.backend.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smarthealthdog.backend.domain.Condition;
import com.smarthealthdog.backend.domain.ConditionTranslation;
import com.smarthealthdog.backend.domain.Diagnosis;
import com.smarthealthdog.backend.domain.Language;
import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.domain.SubmissionStatus;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.dto.diagnosis.get.SubmissionMapper;
import com.smarthealthdog.backend.dto.diagnosis.update.DiagnosisResultDto;
import com.smarthealthdog.backend.dto.diagnosis.update.SubmissionResultRequest;
import com.smarthealthdog.backend.exceptions.InternalServerErrorException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.LanguageRepository;
import com.smarthealthdog.backend.repositories.SubmissionRepository;

@ExtendWith(MockitoExtension.class)
public class SubmissionServiceUT {
    @InjectMocks
    private SubmissionService submissionService;

    @Mock
    private LanguageRepository languageRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private ConditionService conditionService;

    @Mock
    private DiagnosisService diagnosisService;

    @Mock
    private SubmissionMapper submissionMapper;

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

    @Test
    void getSubmissionAndDiagnosesById_ShouldThrowInternalServerErrorException_WhenLanguageNotFound() {
        UUID submissionId = mock(UUID.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.empty());

        assertThrows(InternalServerErrorException.class, () -> {
            submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        });
    }

    @Test
    void getSubmissionAndDiagnosesById_ShouldThrowResourceNotFoundException_WhenSubmissionNotFound() {
        UUID submissionId = mock(UUID.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.of(mock(Language.class)));
        when(submissionRepository.findByIdWithPetAndUser(submissionId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        });
    }

    @Test
    void getSubmissionAndDiagnosesById_ShouldThrowResourceNotFoundException_WhenUserIdDoesNotMatch() {
        UUID submissionId = mock(UUID.class);
        Language mockLanguage = mock(Language.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.of(mockLanguage));

        Submission mockSubmission = mock(Submission.class);
        when(mockSubmission.getPet()).thenReturn(mock(Pet.class));
        when(mockSubmission.getPet().getOwner()).thenReturn(mock(User.class));
        when(mockSubmission.getPet().getOwner().getId()).thenReturn(2L); // Different user ID

        when(submissionRepository.findByIdWithPetAndUser(submissionId)).thenReturn(Optional.of(mockSubmission));

        assertThrows(ResourceNotFoundException.class, () -> {
            submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        });
    }

    @Test
    void getSubmissionAndDiagnosesById_ShouldThrowInternalServerErrorException_WhenDiagnosesAreEmpty() {
        UUID submissionId = mock(UUID.class);
        Language mockLanguage = mock(Language.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.of(mockLanguage));

        Submission mockSubmission = mock(Submission.class);
        Pet mockPet = mock(Pet.class);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockPet.getOwner()).thenReturn(mockUser);
        when(mockSubmission.getPet()).thenReturn(mockPet);

        when(submissionRepository.findByIdWithPetAndUser(submissionId)).thenReturn(Optional.of(mockSubmission));
        when(diagnosisService.getDiagnosesBySubmissionId(submissionId)).thenReturn(List.of()); // Empty diagnoses

        assertThrows(InternalServerErrorException.class, () -> {
            submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        });
    }

    @Test
    void getSubmissionAndDiagnosesById_ShouldThrowInternalServerErrorException_WhenTranslationsAreEmpty() {
        UUID submissionId = mock(UUID.class);
        Language mockLanguage = mock(Language.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.of(mockLanguage));

        Submission mockSubmission = mock(Submission.class);
        Pet mockPet = mock(Pet.class);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockPet.getOwner()).thenReturn(mockUser);
        when(mockSubmission.getPet()).thenReturn(mockPet);

        when(submissionRepository.findByIdWithPetAndUser(submissionId)).thenReturn(Optional.of(mockSubmission));
        Condition mockCondition = mock(Condition.class);
        when(mockCondition.getId()).thenReturn(1);
        Diagnosis mockDiagnosis = mock(Diagnosis.class);
        when(mockDiagnosis.getCondition()).thenReturn(mockCondition);

        when(diagnosisService.getDiagnosesBySubmissionId(submissionId)).thenReturn(List.of(mockDiagnosis));
        when(conditionService.getConditionTranslationsByConditionIdsAndLanguage(
            List.of(1), mockLanguage)).thenReturn(List.of()); // Empty translations

        assertThrows(InternalServerErrorException.class, () -> {
            submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        });
    }

    @Test
    void getSubmissionAndDiagnosesById_ShouldProcessSuccessfully_WhenValidInput() {
        UUID submissionId = mock(UUID.class);
        Language mockLanguage = mock(Language.class);
        when(languageRepository.findByCode("en")).thenReturn(Optional.of(mockLanguage));

        Submission mockSubmission = mock(Submission.class);
        Pet mockPet = mock(Pet.class);
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockPet.getOwner()).thenReturn(mockUser);
        when(mockSubmission.getPet()).thenReturn(mockPet);

        when(submissionRepository.findByIdWithPetAndUser(submissionId)).thenReturn(Optional.of(mockSubmission));
        Condition mockCondition = mock(Condition.class);
        when(mockCondition.getId()).thenReturn(1);
        Diagnosis mockDiagnosis = mock(Diagnosis.class);
        when(mockDiagnosis.getCondition()).thenReturn(mockCondition);

        when(diagnosisService.getDiagnosesBySubmissionId(submissionId)).thenReturn(List.of(mockDiagnosis));
        when(conditionService.getConditionTranslationsByConditionIdsAndLanguage(
            List.of(1), mockLanguage)).thenReturn(List.of(mock(ConditionTranslation.class)));

        submissionService.getSubmissionAndDiagnosesById(submissionId, "en", 1L);
        verify(submissionMapper).toSubmissionDetail(
            any(Submission.class), any(List.class), any(List.class)
        );
    }
}
