package com.smarthealthdog.backend.services;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.github.f4b6a3.uuid.UuidCreator;
import com.smarthealthdog.backend.domain.ConditionTranslation;
import com.smarthealthdog.backend.domain.Diagnosis;
import com.smarthealthdog.backend.domain.Language;
import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.domain.SubmissionStatus;
import com.smarthealthdog.backend.dto.diagnosis.get.SubmissionDetail;
import com.smarthealthdog.backend.dto.diagnosis.get.SubmissionMapper;
import com.smarthealthdog.backend.dto.diagnosis.update.SubmissionResultRequest;
import com.smarthealthdog.backend.exceptions.InternalServerErrorException;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.LanguageRepository;
import com.smarthealthdog.backend.repositories.SubmissionRepository;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final LanguageRepository languageRepository;
    private final SubmissionRepository submissionRepository;
    private final ConditionService conditionService;
    private final DiagnosisService diagnosisService;
    private final SubmissionMapper submissionMapper;

    /**
     * 제출된 진단 결과를 처리하고 제출 상태를 완료로 업데이트합니다.
     * @param submissionId 제출 ID
     * @param request 제출 결과 요청 객체
     * @return 업데이트된 제출 정보
     */
    @Transactional
    public Submission completeDiagnosis(UUID submissionId, SubmissionResultRequest request) {
        Submission submission = getSubmissionById(submissionId);
        if (submission.getStatus() != SubmissionStatus.PROCESSING) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_INPUT);
        }

        if (request == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_INPUT);
        }

        if (request.getResults() == null || request.getResults().isEmpty()) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_INPUT);
        }

        // 1. 진단 결과 처리
        diagnosisService.processInferenceResult(submission, request);

        // 2. 제출 상태 업데이트
        submission.setStatus(SubmissionStatus.COMPLETED); 
        submission.setCompletedAt(Instant.now());
        
        // 3. 제출 정보 저장
        return submissionRepository.save(submission);
    }

    /**
     * 새로운 제출 정보를 생성합니다. (저장 전)
     * @param pet 반려동물 정보
     * @return 생성된 제출 정보
     */
    public Submission createSubmission(Pet pet) {
        Instant now = Instant.now();

        return Submission.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .pet(pet)
                .photoUrl("") // 실제 URL은 S3 업로드 후 설정됩니다.
                .submittedAt(now)
                .build();
    }

    /**
     * 제출을 실패 상태로 업데이트합니다.
     * @param submissionId 제출 ID
     * @param failureReason 실패 사유
     * @return 업데이트된 제출 정보
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Submission failSubmission(Submission submission, String failureReason) {
        if (submission.getStatus() != SubmissionStatus.PROCESSING) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_INPUT);
        }

        submission.setStatus(SubmissionStatus.FAILED);
        submission.setFailureReason(failureReason);
        submission.setCompletedAt(Instant.now());

        return submissionRepository.save(submission);
    }

    /**
     * 진단 ID로 제출 정보를 가져옵니다.
     * @param id 진단 제출 ID
     * @return 제출 정보
     */
    public Submission getSubmissionById(UUID id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    /**
     * 진단 ID로 제출 정보와 관련된 진단 및 번역 정보를 함께 가져옵니다.
     * @param id 진단 제출 ID
     * @return 제출 정보 (진단 및 번역 포함)
     */
    public SubmissionDetail getSubmissionAndDiagnosesById(UUID id, String languageCode, Long userId) {
        if (id == null) {
            throw new IllegalArgumentException("Submission ID must not be null");
        }

        if (languageCode == null) {
            languageCode = "ko"; // Default to Korean if not provided
        }

        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }

        Language preferredLanguage = languageRepository.findByCode(languageCode).
            orElseThrow(() -> new InternalServerErrorException(ErrorCode.INTERNAL_SERVER_ERROR));

        // 1. Load Submission
        Submission submission = submissionRepository.findByIdWithPetAndUser(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        if (!submission.getPet().getOwner().getId().equals(userId)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        // 2. Load Diagnoses
        List<Diagnosis> diagnoses = diagnosisService.getDiagnosesBySubmissionId(id);
        if (diagnoses.isEmpty()) {
            throw new InternalServerErrorException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 3. Extract condition IDs from diagnoses
        List<Integer> conditionIds = diagnoses.stream()
            .map(d -> d.getCondition().getId())
            .distinct()
            .collect(Collectors.toList());

        // 4. Load ConditionTranslations
        List<ConditionTranslation> translations = conditionService.getConditionTranslationsByConditionIdsAndLanguage(conditionIds, preferredLanguage);
        if (translations.isEmpty()) {
            throw new InternalServerErrorException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        return submissionMapper.toSubmissionDetail(submission, diagnoses, translations);
    }

    /**
     * 제출 정보를 저장합니다.
     * @param submission 진단 제출 객체
     * @return 저장된 제출 정보
     */
    @Transactional
    public Submission saveSubmission(Submission submission) {
        return submissionRepository.save(submission);
    }
}
