package com.smarthealthdog.backend.services;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class DevAIDiagnosisClientService implements AIDiagnosisClientService {
    private final FileUploadService fileUploadService;
    private final PetService petService;
    private final SubmissionService submissionService;

    /**
     * 눈 질병 진단을 수행합니다. (개발 및 테스트 환경용)
     * @param imageFile 이미지 파일
     * @param petId 반려동물 ID
     * @param ownerId 소유자 ID
     * @throws IllegalArgumentException petId 또는 ownerId가 null인 경우
     * @throws ResourceNotFoundException 반려동물을 찾을 수 없는 경우
     * @throws InvalidRequestDataException 이미지 업로드에 실패한 경우
     */
    @Override
    public void performEyeDiagnosis(MultipartFile imageFile, Long petId, Long ownerId) {
        if (petId == null || ownerId == null) {
            throw new IllegalArgumentException("Pet ID and Owner ID must not be null for eye diagnosis.");
        }

        Pet pet = petService.get(petId);
        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        Submission submissionBuild = submissionService.createSubmission(pet);
        Submission submission = submissionService.saveSubmission(submissionBuild);

        // 진단 이미지 업로드
        fileUploadService.updateDiagnosisImage(submission.getId(), imageFile);
    }
}
